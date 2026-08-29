package to.etc.sigeto;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SourceSpan;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import to.etc.sigeto.blogextension.BlogExtension;
import to.etc.sigeto.demos.DemoBlock;
import to.etc.sigeto.demos.DemoExtension;
import to.etc.sigeto.emojis.EmojiExtension;
import to.etc.sigeto.notifications.NotificationsExtension;
import to.etc.sigeto.tables.MyTablesExtension;
import to.etc.sigeto.tocextension.TocExtension;
import to.etc.sigeto.utils.Pair;
import to.etc.sigeto.variables.VariableExpander;
import to.etc.sigeto.variables.VariableExtension;
import to.etc.sigeto.variables.VariableNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MarkdownChecker {
	@NonNull
	private final Parser m_parser;

	private final Content m_content;

	@NonNull
	private final MoveMap m_moveMap;

	private final List<Extension> m_extList;

	//@NonNull
	//private final HtmlRenderer m_renderer;

	private ContentItem m_currentItem;

	/** Output-relative directory the page currently being rendered will be written to. */
	private String m_outputDir;

	private List<Message> m_errorList;

	/**
	 * The links found pointing at documents that have moved, to be repaired in
	 * the sources by {@link SourceLinkFixer}.
	 */
	private final List<LinkFix> m_linkFixList = new ArrayList<>();

	/**
	 * The number of lines {@link #parse(File)} removed from the front of the
	 * file (front matter and the blank lines before it), needed to map the
	 * parser's line numbers back onto the real source file.
	 */
	private int m_lineOffset;

	private final Yaml m_yaml = new Yaml();

	private TextContentRenderer m_textRenderer = new TextContentRenderer.Builder().build();

	/** The -include base url that "!demo(path)" tags resolve against, null when the build got none. */
	@Nullable
	private final String m_includeBase;

	/** What a "${name}" variable stands for; returns null for a name that is not defined. */
	@NonNull
	private final Function<String, String> m_variables;

	/**
	 * For every link or image in the document last parsed that used a variable
	 * in its url: the url as it is written in the source file.
	 */
	private final Map<Node, String> m_sourceUrlMap = new IdentityHashMap<>();

	/**
	 * For every link or image in the document last parsed whose url used
	 * variables that are not defined: those names.
	 */
	private final Map<Node, List<String>> m_unknownUrlVariableMap = new IdentityHashMap<>();

	public MarkdownChecker(Content content, @NonNull MoveMap moveMap, @Nullable String includeBase, @NonNull Function<String, String> variables) {
		m_content = content;
		m_moveMap = moveMap;
		m_includeBase = includeBase;
		m_variables = variables;
		//options.set(Parser.EXTENSIONS, Arrays.asList(
		//	TypographicExtension.create(),
		//	SuperscriptExtension.create(),
		//	//SubscriptExtension.create()
		//));

		m_extList = List.of(
			MyTablesExtension.create(),
			StrikethroughExtension.create(),
			TocExtension.create(),
			HeadingAnchorExtension.create(),
			NotificationsExtension.create(),
			EmojiExtension.create(),
			DemoExtension.create(includeBase),
			VariableExtension.create(variables)
		);

		List<Extension> extList = new ArrayList<>(m_extList);
		extList.add(BlogExtension.create());
		m_parser = Parser.builder()
			.extensions(extList)
			.includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)		// Needed to report and repair links by line
			.build();
	}

	private boolean m_debug;

	/**
	 * Render the actual content, writing to the item's own natural output directory.
	 */
	public String renderContent(ContentItem item) throws Exception {
		return renderContent(item, item.getDirectoryPath());
	}

	/**
	 * Render the actual content for output at outputDir, which need not be the
	 * item's own natural directory - this is used to render a story-nested blog
	 * entry a second time into the sitewide global blog timeline namespace.
	 */
	public String renderContent(ContentItem item, String outputDir) throws Exception {
		if(item.getFileType() != ContentFileType.Markdown)
			throw new IllegalStateException(item + " is not markdown");
		m_currentItem = item;
		m_outputDir = outputDir;
		Pair<Node, String> result = parse(item.getFile());
		Node doc = result.getFirst();

		doc.accept(new LinkUpdater(item, outputDir));

		m_debug = item.getType() == ContentType.Blog;

		//if(m_currentItem.getName().startsWith("hp-16702"))
		//	System.out.println();

		List<Extension> extList = new ArrayList<>(m_extList);
		extList.add(BlogExtension.create(item, outputDir));
		HtmlRenderer renderer = HtmlRenderer.builder()
			.extensions(extList)
			.nodeRendererFactory(ctx -> new MdImgRenderer(item, outputDir, ctx))
			.build();
		return renderer.render(doc);
	}

	/**
	 * Pre-scan the content and report any errors.
	 */
	public void scanContent(List<Message> errorList, ContentItem item) throws Exception {
		m_errorList = errorList;
		m_currentItem = item;
		//System.out.println("Pre-parsing " + item.getRelativePath());
		if(item.getFileType() != ContentFileType.Markdown)
			throw new IllegalStateException(item + " is not markdown");
		Pair<Node, String> result = parse(item.getFile());
		Node doc = result.getFirst();
		walkNode(doc, node -> {
			checkNode(node);
		});

		String yamlText = result.getSecond();
		if(null != yamlText && !yamlText.isBlank()) {
			Map<String, Object> map = m_yaml.load(yamlText);
			item.setFrontMatter(map);

			//-- Handle metadata
			Object o = map.get("tags");

			if(o instanceof String) {
				appendTagString(item, (String) o);
			} else if(o instanceof List<?>) {
				List<?> list = (List<?>) o;
				for(Object object : list) {
					if(object instanceof String) {
						appendTagString(item, (String) object);
					} else {
						m_errorList.add(new Message(item, 0, MsgType.Error, "Unexpected type in tags"));
					}
				}
			} else if(o == null) {
				//-- Skip
			} else
				m_errorList.add(new Message(item, 0, MsgType.Error, "Unexpected type in tags"));
		}
	}

	private void appendTagString(ContentItem item, String text) {
		if(text.isBlank())
			return;
		for(String s : text.split(",")) {
			s = s.trim();
			if(!s.isBlank()) {
				ContentTag tag = m_content.getTag(s);
				tag.addItem(item);
			}
		}
	}

	private enum Segment {
		beforeMd,
		inYaml,
		inMarkdown,
	}

	private Pair<Node, String> parse(File file) throws Exception {
		StringBuilder yaml = new StringBuilder();
		StringBuilder markdown = new StringBuilder();
		int skippedLines = 0;

		Segment seg = Segment.beforeMd;
		try(LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while(null != (line = reader.readLine())) {
				if(seg != Segment.inMarkdown) {
					skippedLines++;								// Blank lines and front matter never reach the parser
				}
				switch(seg){
					default:
						throw new IllegalStateException(seg + " ??");

					case beforeMd:
						if(!line.isBlank()) {
							//-- We have data...
							if(line.trim().startsWith("---")) {
								//-- Front matter found -> yaml mode
								seg = Segment.inYaml;
							} else {
								//-- Not fron matter; must be 1st markdown thingy.
								markdown.append(line).append("\n");
								seg = Segment.inMarkdown;
								skippedLines--;						// This line IS passed to the parser
							}
						}
						break;

					case inYaml:
						if(line.trim().startsWith("---")) {
							//-- End of yaml block. Move to markdown
							seg = Segment.inMarkdown;
						} else {
							yaml.append(line).append("\n");
						}
						break;

					case inMarkdown:
						markdown.append(line).append("\n");
						break;
				}
			}
		}

		//-- Parse frontmatter if present
		m_lineOffset = skippedLines;
		Node doc = m_parser.parse(markdown.toString());

		//-- Urls are not part of the inline parse, so expand their variables here, before anything uses them
		m_sourceUrlMap.clear();
		m_unknownUrlVariableMap.clear();
		doc.accept(new VariableExpander(m_variables, m_sourceUrlMap, m_unknownUrlVariableMap));
		return new Pair<>(doc, yaml.toString());
	}


	private void checkNode(Node node) {
		if(node instanceof Link) {
			checkLink((Link) node);
		} else if(node instanceof Image) {
			checkImage((Image) node);
		} else if(node instanceof DemoBlock) {
			checkDemo((DemoBlock) node);
		} else if(node instanceof VariableNode) {
			checkVariable((VariableNode) node);
		} else if(node instanceof Heading) {
			Heading heading = (Heading) node;
			if(m_currentItem.getPageTitle() == null) {
				String hdr = m_textRenderer.render(heading);
				m_currentItem.setPageTitle(hdr);
			}
		}
	}

	/**
	 * A variable that is not defined would leave a literal "${name}" on the
	 * page, which looks like the documentation is broken - so it stops the
	 * build, naming the file and the line, the way a dangling link does.
	 */
	private void checkVariable(VariableNode variable) {
		if(null == m_variables.apply(variable.getName())) {
			m_errorList.add(new Message(m_currentItem, lineNumber(variable), MsgType.Error, unknownVariable(variable.getName())));
		}
	}

	private static String unknownVariable(String name) {
		return "unknown variable ${" + name + "}: define it with -D" + name + "=<value>";
	}

	/**
	 * A "!demo(path)" tag can only be rendered when the build knows what to
	 * resolve its path against, so a page using one without -include is an
	 * error - silently leaving a hole in the page would be worse.
	 */
	private void checkDemo(DemoBlock demo) {
		if(null == m_includeBase) {
			m_errorList.add(new Message(m_currentItem, lineNumber(demo), MsgType.Error,
				"!demo(" + demo.getPath() + ") needs a base url for the application: run the generator with -include <url>"));
			return;
		}
		String problem = DemoBlock.checkSize(demo.getWidth(), "width");
		if(null == problem) {
			problem = DemoBlock.checkSize(demo.getHeight(), "height");
		}
		if(null != problem) {
			m_errorList.add(new Message(m_currentItem, lineNumber(demo), MsgType.Error, problem));
		}
	}

	private void checkImage(Image image) {
		if(reportUnknownUrlVariables(image, "Image"))
			return;
		checkReference(image, image.getDestination(), "Image");
	}

	/**
	 * Check that the thing linked to does exist (if internal), and
	 * replace it with a html link to the generated page.
	 */
	private void checkLink(Link link) {
		if(reportUnknownUrlVariables(link, "Link"))
			return;
		checkReference(link, link.getDestination(), "Link");
	}

	/**
	 * Report the variables used in this node's url that are not defined, and
	 * say whether there were any. The url cannot be resolved when there are,
	 * so it is not checked any further: that would only add a second,
	 * confusing error about a document that does not exist.
	 */
	private boolean reportUnknownUrlVariables(Node node, String kind) {
		List<String> unknownList = m_unknownUrlVariableMap.get(node);
		if(null == unknownList)
			return false;
		for(String name : unknownList) {
			m_errorList.add(new Message(m_currentItem, lineNumber(node), MsgType.Error, kind + " url uses " + unknownVariable(name)));
		}
		return true;
	}

	private void checkReference(Node node, String url, String kind) {
		String path = m_currentItem.resolveURL(url);
		if(null == path)												// External link or in-page anchor: not ours to check
			return;

		ContentItem item = m_content.findItem(path);
		if(null == item) {
			checkMovedReference(node, url, path, kind);
			return;
		}
		m_currentItem.addUsedItem(item, url);
	}

	/**
	 * A link to something that is not there (any more). If the document it
	 * addresses is known to have moved then rewrite the link in the source
	 * (see {@link SourceLinkFixer}) - but still report it as an error, so that
	 * the build stops and the change gets reviewed and committed before the
	 * site is published.
	 */
	private void checkMovedReference(Node node, String url, String path, String kind) {
		String sourceUrl = sourceUrl(node, url);
		String target = m_moveMap.getTarget(path);
		if(null == target) {
			m_errorList.add(new Message(m_currentItem, lineNumber(node), MsgType.Error, kind + " link to unknown document: " + sourceUrl));
			return;
		}

		String newUrl = moveURL(url, target);
		m_linkFixList.add(new LinkFix(m_currentItem, lineNumber(node), sourceUrl, newUrl));
		m_errorList.add(new Message(m_currentItem, lineNumber(node), MsgType.Error,
			kind + " link to moved document: " + sourceUrl + " is now at " + newUrl + " (fixed in the source; review and commit it)"));
	}

	/**
	 * The url the way it is actually written in the source file, which is not
	 * the one in the parsed document when it used a variable. An error has to
	 * name what the author wrote, and {@link SourceLinkFixer} has to be able
	 * to find it back in the file.
	 */
	private String sourceUrl(Node node, String url) {
		String source = m_sourceUrlMap.get(node);
		return null == source ? url : source;
	}

	/**
	 * Rewrite a url that addressed the old location into one addressing the
	 * new one, keeping the way the original was written: a url starting at the
	 * site root stays site-root relative, anything else stays relative to the
	 * page using it.
	 */
	private String moveURL(String url, String target) {
		if(url.startsWith("/"))
			return "/" + target;
		String base = m_currentItem.getDirectoryPath();
		return Util.relativeHref(base, target);
	}

	/**
	 * The 1-based line in the actual source file a node starts on, correcting
	 * for the front matter that was removed before parsing.
	 */
	private int lineNumber(Node node) {
		List<SourceSpan> spanList = node.getSourceSpans();
		if(spanList == null || spanList.isEmpty())
			return 0;
		return spanList.get(0).getLineIndex() + m_lineOffset + 1;
	}

	/**
	 * The links found pointing at documents that have moved, over all files
	 * scanned so far.
	 */
	@NonNull
	public List<LinkFix> getLinkFixList() {
		return m_linkFixList;
	}

	/**
	 * Create a URL relative to the root, using ../.. paths, correct for the
	 * output directory of the page currently being rendered.
	 */
	public String siteURL(String url) {
		return Util.relativeHref(m_outputDir, url);
	}

	static void walkNode(Node node, Consumer<Node> nodeConsumer) {
		Node nd = node.getFirstChild();
		while(nd != null) {
			nodeConsumer.accept(nd);
			walkNode(nd, nodeConsumer);
			nd = nd.getNext();
		}
	}
}

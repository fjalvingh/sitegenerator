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
import org.yaml.snakeyaml.Yaml;
import to.etc.sigeto.blogextension.BlogExtension;
import to.etc.sigeto.emojis.EmojiExtension;
import to.etc.sigeto.notifications.NotificationsExtension;
import to.etc.sigeto.tables.MyTablesExtension;
import to.etc.sigeto.tocextension.TocExtension;
import to.etc.sigeto.utils.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

	public MarkdownChecker(Content content, @NonNull MoveMap moveMap) {
		m_content = content;
		m_moveMap = moveMap;
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
			EmojiExtension.create()
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
		return new Pair<>(doc, yaml.toString());
	}


	private void checkNode(Node node) {
		if(node instanceof Link) {
			checkLink((Link) node);
		} else if(node instanceof Image) {
			checkImage((Image) node);
		} else if(node instanceof Heading) {
			Heading heading = (Heading) node;
			if(m_currentItem.getPageTitle() == null) {
				String hdr = m_textRenderer.render(heading);
				m_currentItem.setPageTitle(hdr);
			}
		}
	}

	private void checkImage(Image image) {
		checkReference(image, image.getDestination(), "Image");
	}

	/**
	 * Check that the thing linked to does exist (if internal), and
	 * replace it with a html link to the generated page.
	 */
	private void checkLink(Link link) {
		checkReference(link, link.getDestination(), "Link");
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
		String target = m_moveMap.getTarget(path);
		if(null == target) {
			m_errorList.add(new Message(m_currentItem, lineNumber(node), MsgType.Error, kind + " link to unknown document: " + url));
			return;
		}

		String newUrl = moveURL(url, target);
		m_linkFixList.add(new LinkFix(m_currentItem, lineNumber(node), url, newUrl));
		m_errorList.add(new Message(m_currentItem, lineNumber(node), MsgType.Error,
			kind + " link to moved document: " + url + " is now at " + newUrl + " (fixed in the source; review and commit it)"));
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

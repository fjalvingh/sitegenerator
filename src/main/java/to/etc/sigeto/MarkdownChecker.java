package to.etc.sigeto;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import to.etc.sigeto.notifications.NotificationsExtension;
import to.etc.sigeto.tables.MyTablesExtension;
import to.etc.sigeto.tocextension.TocExtension;
import to.etc.sigeto.utils.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MarkdownChecker {
	@NonNull
	private final Parser m_parser;

	private final Content m_content;

	private final List<Extension> m_extList;

	//@NonNull
	//private final HtmlRenderer m_renderer;

	private ContentItem m_currentItem;

	private List<Message> m_errorList;

	private final Yaml m_yaml = new Yaml();

	private TextContentRenderer m_textRenderer = new TextContentRenderer.Builder().build();

	public MarkdownChecker(Content content) {
		m_content = content;
		//MutableDataSet options = new MutableDataSet();
		//options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
		//options.set(TablesExtension.CLASS_NAME, "ui-tbl");

		//-- Set this if we want h1 levels to be shown in the toc too. Usually h1 is the document title.
		//options.set(TocExtension.LEVELS, 0x1e);

		//options.set(Parser.EXTENSIONS, Arrays.asList(
		//	TablesExtension.create(),
		//	EmojiExtension.create(),
		//	TypographicExtension.create(),
		//	MdLinkToGeneratedLinkExtension.create(),
		//	MdFixImgExtension.create(this),
		//	TocExtension.create(),
		//	SuperscriptExtension.create(),
		//	//SubscriptExtension.create()
		//	StrikethroughSubscriptExtension.create()
		//));

		m_extList = List.of(
			MyTablesExtension.create(),
			StrikethroughExtension.create(),
			TocExtension.create(),
			HeadingAnchorExtension.create(),
			NotificationsExtension.create()
		);

		m_parser = Parser.builder()
			.extensions(m_extList)
			.build();
	}

	private boolean m_debug;

	/**
	 * Render the actual content.
	 */
	public String renderContent(ContentItem item) throws Exception {
		if(item.getFileType() != ContentFileType.Markdown)
			throw new IllegalStateException(item + " is not markdown");
		m_currentItem = item;
		Pair<Node, String> result = parse(item.getFile());
		Node doc = result.getFirst();

		doc.accept(new LinkUpdater());

		m_debug = item.getType() == ContentType.Blog;

		//walkNode(doc, node -> {
		//	rewriteNode(node);
		//});
		//replaceContentHolders(item, doc);

		if(m_currentItem.getName().startsWith("hp-16702"))
			System.out.println();

		HtmlRenderer renderer = HtmlRenderer.builder()
			.extensions(m_extList)
			.nodeRendererFactory(ctx -> new MdImgRenderer(m_content, item, ctx))
			.build();
		return renderer.render(doc);
	}

	//private void replaceContentHolders(ContentItem item, Node nd) {
	//	if(nd instanceof Text t) {
	//		BasedSequence chars = t.getChars();
	//		String s = chars.toString();
	//		if(s.contains("{{blog")) {
	//			insertBlobHere(item, nd.getParent());
	//			return;
	//		}
	//
	//		if(m_debug) {
	//			System.out.println("text");
	//		}
	//	}
	//
	//	Node fc = nd.getFirstChild();
	//	while(null != fc) {
	//		replaceContentHolders(item, fc);
	//		fc = fc.getNext();
	//	}
	//}

	private void insertBlobHere(ContentItem item, Node nd) {
		List<ContentLevel> bll = new ArrayList<>(item.getLevel().getBlogEntryList());
		if(bll.isEmpty()) {
			nd.unlink();
			return;
		}

		bll.sort(Comparator.comparing(ContentLevel::getName).reversed());

		//-- Sort all blog items
		Node curr = nd;
		for(ContentLevel level : bll) {
			ContentItem rootItem = level.getRootItem();

			curr = appendBlogEntry(curr, rootItem);
		}
		//nd.unlink();
	}

	private Node appendBlogEntry(Node nd, ContentItem rootItem) {
		//BasedSequence bs = BasedSequence.of(rootItem.getPageTitle());
		//Heading hd = new Heading();
		//hd.setLevel(2);
		//nd.insertAfter(hd);
		//Text txt = new Text(bs);
		//hd.appendChild(txt);
		//
		//return hd;
		return null;
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

		Segment seg = Segment.beforeMd;
		try(LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while(null != (line = reader.readLine())) {
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
		String url = image.getDestination();
		if(!Content.isRelativePath(url))
			return;

		ContentItem item = findItemByURL(url);
		if(null == item) {
			m_errorList.add(new Message(m_currentItem, image.getSourceSpans(), MsgType.Error, "Image link to unknown document: " + url));
			return;
		}
		m_currentItem.addUsedItem(item, url);
	}

	/**
	 * Check that the thing linked to does exist (if internal), and
	 * replace it with a html link to the generated page.
	 */
	private void checkLink(Link link) {
		if(m_currentItem.getName().startsWith("hp-16702"))
			System.out.println();
		String url = link.getDestination();
		if(!Content.isRelativePath(url))
			return;

		ContentItem item = findItemByURL(url);
		if(null == item) {
			m_errorList.add(new Message(m_currentItem, link.getSourceSpans(), MsgType.Error, "Link to unknown document: " + url));
			return;
		}
		m_currentItem.addUsedItem(item, url);
	}

	@Nullable
	public ContentItem findItemByURL(String url) {
		if(!Content.isRelativePath(url))
			return null;

		String fullPath;
		if(url.startsWith("/")) {
			fullPath = url.substring(1);
		} else {
			//-- Relative wrt the parent
			Path path = Path.of(m_currentItem.getDirectoryPath());
			Path resolvedPath = path.resolve(url).normalize();
			fullPath = resolvedPath.toString();
		}

		ContentItem item = m_content.findItem(fullPath);
		return item;
	}

	/**
	 * Create a URL relative to the root, using ../.. paths.
	 */
	public String siteURL(String url) {
		String rp = m_currentItem.getRelativePath();
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i < rp.length(); i++) {
			char c = rp.charAt(i);
			if(c == '/') {
				sb.append("../");
			}
		}
		sb.append(url);
		return sb.toString();
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

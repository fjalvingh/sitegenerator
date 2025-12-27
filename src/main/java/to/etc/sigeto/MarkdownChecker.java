package to.etc.sigeto;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Pair;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import to.etc.sigeto.MdImageRewriter.MdFixImgExtension;
import to.etc.sigeto.MdToGeneratedLinkResolver.MdLinkToGeneratedLinkExtension;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MarkdownChecker {
	private final @NotNull Parser m_parser;

	private final Content m_content;

	private final @NotNull HtmlRenderer m_renderer;

	private ContentItem m_currentItem;

	private List<Message> m_errorList;

	private Dimension m_maxImageSize = new Dimension(900, 900);

	private final Yaml m_yaml = new Yaml();

	public MarkdownChecker(Content content) {
		m_content = content;
		MutableDataSet options = new MutableDataSet();
		options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
		options.set(TablesExtension.CLASS_NAME, "ui-tbl");

		//-- Set this if we want h1 levels to be shown in the toc too. Usually h1 is the document title.
		//options.set(TocExtension.LEVELS, 0x1e);

		options.set(Parser.EXTENSIONS, Arrays.asList(
			TablesExtension.create(),
			EmojiExtension.create(),
			TypographicExtension.create(),
			MdLinkToGeneratedLinkExtension.create(),
			MdFixImgExtension.create(this),
			TocExtension.create(),
			SuperscriptExtension.create(),
			//SubscriptExtension.create()
			StrikethroughSubscriptExtension.create()
		));
		m_parser = Parser.builder(options).build();
		m_renderer = HtmlRenderer.builder(options).build();
	}

	private boolean m_debug;

	/**
	 * Render the actual content.
	 */
	public String renderContent(ContentItem item) throws Exception {
		if(item.getFileType() != ContentFileType.Markdown)
			throw new IllegalStateException(item + " is not markdown");
		m_currentItem = item;
		Pair<Document, String> result = parse(item.getFile());
		Document doc = result.getFirst();

		m_debug = item.getType() == ContentType.Blog;

		//walkNode(doc, node -> {
		//	rewriteNode(node);
		//});
		replaceContentHolders(item, doc);

		if(m_currentItem.getName().startsWith("hp-16702"))
			System.out.println();
		return m_renderer.render(doc);
	}

	private void replaceContentHolders(ContentItem item, Node nd) {
		if(nd instanceof Text t) {
			BasedSequence chars = t.getChars();
			String s = chars.toString();
			if(s.contains("{{blog")) {
				insertBlobHere(item, nd.getParent());
				return;
			}

			if(m_debug) {
				System.out.println("text");
			}
		}

		Node fc = nd.getFirstChild();
		while(null != fc) {
			replaceContentHolders(item, fc);
			fc = fc.getNext();
		}
	}

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
		BasedSequence bs = BasedSequence.of(rootItem.getPageTitle());
		Heading hd = new Heading();
		hd.setLevel(2);
		nd.insertAfter(hd);
		Text txt = new Text(bs);
		hd.appendChild(txt);

		return hd;
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
		Pair<Document, String> result = parse(item.getFile());
		Document doc = result.getFirst();
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

	private Pair<Document, String> parse(File file) throws Exception {
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
		Document doc = m_parser.parse(markdown.toString());
		return new Pair<>(doc, yaml.toString());
	}


	private void checkNode(Node node) {
		if(node instanceof Link) {
			checkLink((Link) node);
		} else if(node instanceof Image) {
			checkImage((Image) node);
		} else if(node instanceof Heading) {
			Heading heading = (Heading) node;
			if(m_currentItem.getPageTitle() == null)
				m_currentItem.setPageTitle(heading.getText().unescape());
		}
	}

	private void checkImage(Image image) {
		String url = image.getUrl().unescape();
		if(!Content.isRelativePath(url))
			return;

		ContentItem item = findItemByURL(url);
		if(null == item) {
			m_errorList.add(new Message(m_currentItem, image.getLineNumber(), MsgType.Error, "Image link to unknown document: " + url));
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
		String url = link.getUrl().unescape();
		if(!Content.isRelativePath(url))
			return;

		ContentItem item = findItemByURL(url);
		if(null == item) {
			m_errorList.add(new Message(m_currentItem, link.getLineNumber(), MsgType.Error, "Link to unknown document: " + url));
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
		var iterator = node.getChildIterator();
		while(iterator.hasNext()) {
			var child = iterator.next();
			nodeConsumer.accept(child);
			walkNode(child, nodeConsumer);
		}
	}

	public Dimension getMaxImageSize() {
		return m_maxImageSize;
	}
}

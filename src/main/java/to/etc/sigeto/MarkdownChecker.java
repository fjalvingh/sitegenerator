package to.etc.sigeto;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import to.etc.sigeto.MdImageRewriter.MdFixImgExtension;
import to.etc.sigeto.MdToGeneratedLinkResolver.MdLinkToGeneratedLinkExtension;

import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MarkdownChecker {
	private final @NotNull Parser m_parser;

	private final Content m_content;

	private final @NotNull HtmlRenderer m_renderer;

	private ContentItem m_currentItem;

	private List<Message> m_errorList;

	private Dimension m_maxImageSize = new Dimension(900, 900);

	public MarkdownChecker(Content content) {
		m_content = content;
		MutableDataSet options = new MutableDataSet();
		options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
		options.set(Parser.EXTENSIONS, Arrays.asList(
			TablesExtension.create(),
			StrikethroughExtension.create(),
			EmojiExtension.create(),
			TypographicExtension.create(),
			MdLinkToGeneratedLinkExtension.create(),
			MdFixImgExtension.create(this),
			TocExtension.create()
		));
		m_parser = Parser.builder(options).build();
		m_renderer = HtmlRenderer.builder(options).build();
	}

	/**
	 * Render the actual content.
	 */
	public String renderContent(ContentItem item) throws Exception {
		if(item.getFileType() != ContentFileType.Markdown)
			throw new IllegalStateException(item + " is not markdown");
		m_currentItem = item;
		String text = Util.readFileAsString(item.getFile());
		Document doc = m_parser.parse(text);

		//walkNode(doc, node -> {
		//	rewriteNode(node);
		//});
		return m_renderer.render(doc);
	}

	//private void rewriteNode(Node node) {
	//	if(node instanceof Link) {
	//		checkLink((Link) node);
	//	}
	//}

	/**
	 * Pre-scan the content and report any errors.
	 */
	public void scanContent(List<Message> errorList, ContentItem item) throws Exception {
		m_errorList = errorList;
		m_currentItem = item;
		System.out.println("Pre-parsing " + item.getRelativePath());
		if(item.getFileType() != ContentFileType.Markdown)
			throw new IllegalStateException(item + " is not markdown");
		String text = Util.readFileAsString(item.getFile());
		Document doc = m_parser.parse(text);
		walkNode(doc, node -> {
			checkNode(node);
		});
	}

	private void checkNode(Node node) {
		if(node instanceof Link) {
			checkLink((Link) node);
		} else if(node instanceof Heading) {
			Heading heading = (Heading) node;
			if(m_currentItem.getPageTitle() == null)
				m_currentItem.setPageTitle(heading.getText().unescape());
		}
	}

	/**
	 * Check that the thing linked to does exist (if internal), and
	 * replace it with a html link to the generated page.
	 */
	private void checkLink(Link link) {
		String url = link.getUrl().unescape();
		if(!Content.isRelativePath(url))
			return;

		ContentItem item = findItemByURL(url);

		//if(url.indexOf(':') != -1)									// http(s): url?
		//	return;													// We cannot check those currently
		//if(url.startsWith("#"))
		//	return;
		//
		//String fullPath;
		//if(url.startsWith("/")) {
		//	fullPath = url.substring(1);
		//} else {
		//	//-- Relative wrt the parent
		//	Path path = Path.of(m_currentItem.getDirectoryPath());
		//	Path resolvedPath = path.resolve(url).normalize();
		//	fullPath = resolvedPath.toString();
		//}
		//
		//ContentItem item = m_content.findItem(fullPath);
		if(null == item) {
			m_errorList.add(new Message(m_currentItem, link.getLineNumber(), MsgType.Error, "Link to unknown document: " + url));
			return;
		}

		////-- It worked. Is this a Markdown link?
		//if(item.getType() == ContentType.Markdown) {
		//	//-- Change the URL to the generated target
		//	String targetURL = Util.getFilenameSansExtension(url) + ".html";
		//	link.setUrl(BasedSequence.of(targetURL));
		//	link.setPageRef(BasedSequence.of(targetURL));
		//}
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

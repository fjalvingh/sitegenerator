package to.etc.sigeto;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MarkdownChecker {
	private final @NotNull Parser m_parser;

	private final Content m_content;

	private ContentItem m_currentItem;

	private List<Message> m_errorList;

	public MarkdownChecker(Content content) {
		m_content = content;
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(
			TablesExtension.create(),
			StrikethroughExtension.create(),
			TypographicExtension.create()

		));
		m_parser = Parser.builder(options).build();
	}

	public void scanContent(List<Message> errorList, ContentItem item) throws Exception {
		m_errorList = errorList;
		m_currentItem = item;
		System.out.println("Pre-parsing " + item.getRelativePath());
		if(item.getType() != ContentType.Markdown)
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


		}

	}

	private void checkLink(Link link) {
		String url = link.getUrl().unescape();
		if(url.indexOf(':') != -1)									// http(s): url?
			return;													// We cannot check those currently
		if(url.startsWith("#"))
			return;

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
		if(null == item) {
			m_errorList.add(new Message(m_currentItem, link.getLineNumber(), MsgType.Error, "Link to unknown document: " + url + " " + fullPath));
			return;
		}
	}

	static void walkNode(Node node, Consumer<Node> nodeConsumer) {
		var iterator = node.getChildIterator();
		while(iterator.hasNext()) {
			var child = iterator.next();
			nodeConsumer.accept(child);
			walkNode(child, nodeConsumer);
		}
	}
}

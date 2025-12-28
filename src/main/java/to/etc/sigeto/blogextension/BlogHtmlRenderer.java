package to.etc.sigeto.blogextension;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import to.etc.sigeto.ContentItem;
import to.etc.sigeto.ContentLevel;
import to.etc.sigeto.LinkUpdater;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final public class BlogHtmlRenderer implements NodeRenderer {
	private final HtmlWriter m_htmlWriter;

	private final HtmlNodeRendererContext m_context;

	private final ContentItem m_item;

	public BlogHtmlRenderer(HtmlNodeRendererContext context, ContentItem item) {
		m_htmlWriter = context.getWriter();
		m_context = context;
		m_item = item;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(BlogNode.class);
	}

	@Override
	public void render(Node node) {
		BlogNode toc = (BlogNode) node;

		List<ContentLevel> ble = m_item.getLevel().getBlogEntryList();
		if(ble.isEmpty())
			return;
		List<ContentLevel> list = new ArrayList<>(ble);
		list.sort(Comparator.comparing(ContentLevel::getName).reversed());

		m_htmlWriter.tag("ul");
		for(ContentLevel blog : list) {
			ContentItem rootItem = blog.getRootItem();
			if(null != rootItem) {
				renderBlogEntry(blog, rootItem);
			}
		}
		m_htmlWriter.tag("/ul");
	}

	private void renderBlogEntry(ContentLevel blog, ContentItem rootItem) {
		String blogDate = blog.getName();					// Is the name of the dir, in yyyymmdd format

		String date = blogDate.substring(0, 4)
			+ "/" + blogDate.substring(4, 6)
			+ "/" + blogDate.substring(6, 8);

		m_htmlWriter.tag("li");

		m_htmlWriter.tag("span", Map.of("class", "blg-date"));
		m_htmlWriter.text(date);
		m_htmlWriter.tag("/span");

		m_htmlWriter.text(" ");

		String url = LinkUpdater.fixLink(blog.getName() + "/" + rootItem.getName());
		m_htmlWriter.tag("a", Map.of("href", url));
		m_htmlWriter.text(rootItem.getPageTitle());
		m_htmlWriter.tag("/a");

		m_htmlWriter.tag("/li");
		m_htmlWriter.line();
	}
}

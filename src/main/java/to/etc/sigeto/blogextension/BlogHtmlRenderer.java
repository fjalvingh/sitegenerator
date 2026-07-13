package to.etc.sigeto.blogextension;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import to.etc.sigeto.ContentItem;
import to.etc.sigeto.ContentLevel;
import to.etc.sigeto.Util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders the [blog] marker (list of blog entries directly nested under the
 * current page, linking to their local/story-scoped pages) and the
 * [blog global] marker (sitewide chronological list of every blog entry,
 * linking to each entry's global page - see ContentLevel.getGlobalOutputPath()).
 */
final public class BlogHtmlRenderer implements NodeRenderer {
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private final HtmlWriter m_htmlWriter;

	private final ContentItem m_item;

	private final String m_outputDir;

	public BlogHtmlRenderer(HtmlNodeRendererContext context, ContentItem item, String outputDir) {
		m_htmlWriter = context.getWriter();
		m_item = item;
		m_outputDir = outputDir;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(BlogNode.class);
	}

	@Override
	public void render(Node node) {
		BlogNode blogNode = (BlogNode) node;
		if(blogNode.getOptions().contains("global")) {
			renderGlobalList();
		} else {
			renderLocalList();
		}
	}

	private void renderLocalList() {
		List<ContentLevel> ble = m_item.getLevel().getBlogEntryList();
		if(ble.isEmpty())
			return;
		List<ContentLevel> list = new ArrayList<>(ble);
		list.sort(Comparator.comparing(ContentLevel::getBlogDate).reversed());

		m_htmlWriter.tag("ul");
		for(ContentLevel blog : list) {
			ContentItem rootItem = blog.getRootItem();
			if(null != rootItem) {
				renderBlogEntry(blog, rootItem, rootItem.getRelativeTargetPath());
			}
		}
		m_htmlWriter.tag("/ul");
	}

	private void renderGlobalList() {
		List<ContentLevel> list = new ArrayList<>(m_item.getContent().getAllBlogEntries());
		if(list.isEmpty())
			return;
		list.sort(Comparator.comparing(ContentLevel::getBlogDate).reversed());

		m_htmlWriter.tag("ul");
		for(ContentLevel blog : list) {
			ContentItem rootItem = blog.getRootItem();
			String globalPath = blog.getGlobalOutputPath();
			if(null != rootItem && null != globalPath) {
				renderBlogEntry(blog, rootItem, globalPath);
			}
		}
		m_htmlWriter.tag("/ul");
	}

	private void renderBlogEntry(ContentLevel blog, ContentItem rootItem, String targetPath) {
		LocalDate blogDate = blog.getBlogDate();

		m_htmlWriter.tag("li");

		m_htmlWriter.tag("span", Map.of("class", "blg-date"));
		m_htmlWriter.text(null == blogDate ? "" : DATE_FORMAT.format(blogDate));
		m_htmlWriter.tag("/span");

		m_htmlWriter.text(" ");

		String href = Util.relativeHref(m_outputDir, targetPath);
		m_htmlWriter.tag("a", Map.of("href", href));
		m_htmlWriter.text(rootItem.getPageTitle());
		m_htmlWriter.tag("/a");

		m_htmlWriter.tag("/li");
		m_htmlWriter.line();
	}
}

package to.etc.sigeto;

import java.util.ArrayList;
import java.util.List;

public class PageModel {
	private final String m_content;

	private final MarkdownChecker m_markdown;

	private final ContentItem m_item;

	public PageModel(String content, MarkdownChecker markdown, ContentItem item) {
		m_content = content;
		m_markdown = markdown;
		m_item = item;
	}

	public String getContent() {
		return m_content;
	}

	public ContentItem getItem() {
		return m_item;
	}

	public String getTitle() {
		return m_item.getPageTitle() == null ? "Content page" : m_item.getPageTitle();
	}

	public String siteURL(String url) {
		return m_markdown.siteURL(url);
	}

	/**
	 * Get path from high to low, for breadcrumbs.
	 */
	public List<ContentItem> getBreadcrumbPath() {
		List<ContentItem> list = new ArrayList<>();
		ContentLevel level = m_item.getLevel();
		if(level.getParentLevel() == null) {
			return list;
		}
		while(level != null) {
			ContentItem rootItem = level.getRootItem();
			if(rootItem != null) {
				list.add(0, rootItem);
			}
			level = level.getParentLevel();
		}
		return list;
	}

}

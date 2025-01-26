package to.etc.sigeto;

import java.util.ArrayList;
import java.util.List;

public class PageModel {
	private final Content m_siteContent;

	private final String m_content;

	private final MarkdownChecker m_markdown;

	private final ContentItem m_item;

	public PageModel(Content siteContent, String content, MarkdownChecker markdown, ContentItem item) {
		m_siteContent = siteContent;
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

	public Menu getMenu() {
		return m_siteContent.getMenu();
	}

	public MenuItem getMenuRoot() {
		return getMenu().getRoot();
	}

	public String siteURL(String url) {
		return m_markdown.siteURL(url);
	}

	public boolean isCurrentItem(MenuItem item) {
		if(null == item) {
			return false;
		}
		return m_item == item.getItem();
	}

	public boolean containsCurrentItem(MenuItem item) {
		ContentLevel menuLevel = item.getLevel();				// The level for the menu
		return menuLevel.isInside(item.getLevel());
	}

	public boolean mustShowItem(MenuItem menu) {
		//if(m_item.getRelativePath().startsWith("index/pdp-11"))
		//	System.out.println();
		if(null == menu) {
			return false;
		}
		ContentItem menuItem = menu.getItem();
		if(menuItem == null) {							// The root item contains all
			return true;
		}

		ContentLevel currentItemLevel = m_item.getLevel();

		return menuItem.getLevel().isInside(currentItemLevel);
		//return currentItemLevel.isInside(menuItem.getLevel());
	}

	/**
	 * Get path from high to low, for breadcrumbs.
	 */
	public List<ContentItem> getBreadcrumbPath() {
		//if(m_item.getRelativePath().startsWith("index/pdp-1144.md")) {
		//	System.out.println();
		//}
		List<ContentItem> list = new ArrayList<>();
		ContentLevel level = m_item.getLevel();
		if(level.getParentLevel() == null) {
			//-- If this is not the root index page then always add it
			if(!m_item.isIndexItem()) {
				list.add(m_siteContent.getIndexRootLevel().getRootItem());
			}
			return list;
		}
		while(level != null && level != m_siteContent.getPageRootLevel()) {
			ContentItem rootItem = level.getRootItem();
			if(rootItem != null) {
				list.add(0, rootItem);
			}
			level = level.getParentLevel();
		}
		return list;
	}

}

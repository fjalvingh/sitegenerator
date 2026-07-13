package to.etc.sigeto;

import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PageModel {
	private final Content m_siteContent;

	private final String m_content;

	private final MarkdownChecker m_markdown;

	private final ContentItem m_item;

	@Nullable
	private final String m_previousHref;

	@Nullable
	private final String m_previousTitle;

	@Nullable
	private final String m_nextHref;

	@Nullable
	private final String m_nextTitle;

	public PageModel(Content siteContent, String content, MarkdownChecker markdown, ContentItem item) {
		this(siteContent, content, markdown, item, null, null, null, null);
	}

	public PageModel(Content siteContent, String content, MarkdownChecker markdown, ContentItem item,
		@Nullable String previousHref, @Nullable String previousTitle, @Nullable String nextHref, @Nullable String nextTitle) {
		m_siteContent = siteContent;
		m_content = content;
		m_markdown = markdown;
		m_item = item;
		m_previousHref = previousHref;
		m_previousTitle = previousTitle;
		m_nextHref = nextHref;
		m_nextTitle = nextTitle;
	}

	@Nullable
	public String getPreviousHref() {
		return m_previousHref;
	}

	@Nullable
	public String getPreviousTitle() {
		return m_previousTitle;
	}

	@Nullable
	public String getNextHref() {
		return m_nextHref;
	}

	@Nullable
	public String getNextTitle() {
		return m_nextTitle;
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
		List<ContentItem> list = new ArrayList<>();
		ContentLevel level = m_item.getLevel();					// This is the item's level, we do not need that.
		for(;;) {
			level = level.getParentLevel();
			if(null == level)
				break;
			ContentItem rootItem = level.getRootItem();
			if(rootItem != null) {
				list.add(0, rootItem);
			}
		}
		return list;
	}

}

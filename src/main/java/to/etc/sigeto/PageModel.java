package to.etc.sigeto;

import org.eclipse.jdt.annotation.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PageModel {
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private final Content m_siteContent;

	private final String m_content;

	/** The directory the page is written to, relative to the site root; all links on the page are relative to it. */
	private final String m_outputDir;

	private final ContentItem m_item;

	@Nullable
	private final String m_previousHref;

	@Nullable
	private final String m_previousTitle;

	@Nullable
	private final String m_nextHref;

	@Nullable
	private final String m_nextTitle;

	/** Lazily resolved menu item for this page, see {@link #getCurrentMenuItem()}. */
	@Nullable
	private MenuItem m_currentMenuItem;

	private boolean m_currentMenuItemResolved;

	public PageModel(Content siteContent, String content, String outputDir, ContentItem item) {
		this(siteContent, content, outputDir, item, null, null, null, null);
	}

	public PageModel(Content siteContent, String content, String outputDir, ContentItem item,
		@Nullable String previousHref, @Nullable String previousTitle, @Nullable String nextHref, @Nullable String nextTitle) {
		m_siteContent = siteContent;
		m_content = content;
		m_outputDir = outputDir;
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

	/**
	 * T for a page the generator made itself, like the tag page, rather than
	 * one written in the content.
	 */
	public boolean isGenerated() {
		return m_item.getFileType() == ContentFileType.Generated;
	}

	public String getTitle() {
		return m_item.getPageTitle() == null ? "Content page" : m_item.getPageTitle();
	}

	public String getCreatedDateText() {
		return DATE_FORMAT.format(m_item.getCreatedDate());
	}

	public String getModifiedDateText() {
		return DATE_FORMAT.format(m_item.getModifiedDate());
	}

	public Menu getMenu() {
		return m_siteContent.getMenu();
	}

	public MenuItem getMenuRoot() {
		return getMenu().getRoot();
	}

	/**
	 * Create a URL relative to the root, using ../.. paths, correct for the
	 * output directory of this page.
	 */
	public String siteURL(String url) {
		return Util.relativeHref(m_outputDir, url);
	}

	/**
	 * The tags of this page, in the order its front matter lists them.
	 */
	public List<ContentTag> getTagList() {
		return m_item.getTagList();
	}

	/**
	 * The link to the tag's section on the tag page.
	 */
	public String tagHref(ContentTag tag) {
		return siteURL(TagPage.FILE_NAME + "#" + tag.getAnchor());
	}

	/**
	 * The link to the tag page, or null when the site has no tags.
	 */
	@Nullable
	public String getTagPageHref() {
		ContentItem tagPage = m_siteContent.getTagPage();
		return null == tagPage ? null : siteURL(tagPage.getRelativeTargetPath());
	}

	/**
	 * The menu item for the page being rendered, or - for a page that is not in
	 * the menu itself, like a blog entry - the closest page above it that is.
	 * Null when the page has nothing at all above it in the menu.
	 */
	@Nullable
	public MenuItem getCurrentMenuItem() {
		if(!m_currentMenuItemResolved) {
			m_currentMenuItemResolved = true;
			m_currentMenuItem = findCurrentMenuItem();
		}
		return m_currentMenuItem;
	}

	@Nullable
	private MenuItem findCurrentMenuItem() {
		Menu menu = getMenu();
		MenuItem mi = menu.findItem(m_item);
		if(null != mi) {
			return mi;
		}

		//-- Not in the menu (hidden, or a blog entry): use the closest enclosing page that is
		ContentLevel level = m_item.getLevel();
		while(null != level) {
			mi = menu.findItem(level.getRootItem());
			if(null != mi) {
				return mi;
			}
			level = level.getParentLevel();
		}
		return null;
	}

	public boolean isCurrentItem(@Nullable MenuItem item) {
		if(null == item) {
			return false;
		}
		return m_item == item.getItem();
	}

	/**
	 * T if the item is on the path from the menu root to the page being
	 * rendered, so that its children need to be shown too.
	 */
	public boolean isOpenItem(@Nullable MenuItem item) {
		if(null == item) {
			return false;
		}
		if(item.isRoot()) {
			return true;
		}
		MenuItem current = getCurrentMenuItem();
		while(null != current) {
			if(current == item) {
				return true;
			}
			current = current.getParent();
		}
		return false;
	}

	/**
	 * T if this item is part of the menu for the page being rendered: all top
	 * level items are always there, and below that only the children of the
	 * items on the path to the current page - so the page sees everything
	 * above it, all of its siblings and its own children.
	 */
	public boolean mustShowItem(@Nullable MenuItem menu) {
		if(null == menu) {
			return false;
		}
		MenuItem parent = menu.getParent();
		if(null == parent) {							// The root item contains all
			return true;
		}
		if(parent.isRoot()) {							// Top level items are always shown
			return true;
		}
		return isOpenItem(parent);
	}

	/**
	 * The link to a menu item's page, relative to the page being rendered.
	 */
	public String menuHref(MenuItem item) {
		return siteURL(item.getTargetPath());
	}

	/**
	 * The link to the generated menu.json, for the javascript menu.
	 */
	public String getMenuJsonHref() {
		return siteURL(MenuJsonWriter.FILE_NAME);
	}

	/**
	 * The prefix that gets you from this page back to the site root, so that
	 * javascript can make links out of the site root relative paths in
	 * menu.json.
	 */
	public String getSiteRootHref() {
		return siteURL("");
	}

	/**
	 * The path of this page itself, relative to the site root - the form links
	 * in menu.json have, so javascript can recognise the current page.
	 */
	public String getCurrentPagePath() {
		return m_item.getRelativeTargetPath();
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

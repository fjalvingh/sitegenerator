package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A single item in the generated menu.
 */
final public class MenuItem {
	private final ContentLevel m_level;

	private final ContentItem m_item;

	/** The title to use for the entry */
	private final String m_title;

	/**
	 * The explicit sort order for this item, from the front matter's menu.sort
	 * or from the numeric prefix of the item's directory or file name; null when
	 * the item has no sort indication at all, in which case it sorts by title
	 * after all items that do have one.
	 */
	@Nullable
	private final String m_sortKey;

	private final int m_itemLevel;

	private int m_menuIndex;

	private List<MenuItem> m_subItemList = new ArrayList<>();

	@Nullable
	private MenuItem m_parent;

	public MenuItem(@Nullable MenuItem parent, @NonNull ContentLevel level, @NonNull ContentItem item, @NonNull String title, @Nullable String sortKey, int itemLevel) {
		if(level == null || item == null || title == null) {
			throw new MessageException("Menu item cannot be created for " + level + ": missing level, item or title (probably a content directory without an index markdown file)");
		}
		m_parent = parent;
		m_level = level;
		m_item = item;
		m_title = title;
		m_sortKey = sortKey;
		m_itemLevel = itemLevel;
	}

	public ContentItem getItem() {
		return m_item;
	}

	/**
	 * The item this one is nested under; null for the (invisible) menu root.
	 */
	@Nullable
	public MenuItem getParent() {
		return m_parent;
	}

	/**
	 * T if this is the invisible root of the menu tree; its children are the
	 * top level menu items.
	 */
	public boolean isRoot() {
		return m_parent == null;
	}

	/**
	 * The path of the generated page for this item, relative to the site root.
	 */
	@NonNull
	public String getTargetPath() {
		return m_item.getRelativeTargetPath();
	}

	public boolean hasChildren() {
		return !m_subItemList.isEmpty();
	}

	public String getTitle() {
		return m_title;
	}

	/**
	 * The item's explicit sort order, or null when it has none.
	 */
	@Nullable
	public String getSortKey() {
		return m_sortKey;
	}

	/**
	 * The order of the items inside one menu level: everything with a sort order
	 * first, in that order, and everything without one after it, by title.
	 */
	public static final Comparator<MenuItem> BY_MENU_ORDER = (a, b) -> {
		String as = a.m_sortKey;
		String bs = b.m_sortKey;
		if(as == null || bs == null) {
			if(as != null)
				return -1;
			if(bs != null)
				return 1;
			return compareTitle(a, b);
		}
		int res = compareSortKey(as, bs);
		return res != 0 ? res : compareTitle(a, b);
	};

	private static int compareTitle(MenuItem a, MenuItem b) {
		int res = a.m_title.compareToIgnoreCase(b.m_title);
		return res != 0 ? res : a.m_title.compareTo(b.m_title);
	}

	/**
	 * Compare two sort orders: numerically when both are numbers - so that 100
	 * comes after 20 - and as text otherwise, with numbers before text.
	 */
	private static int compareSortKey(String a, String b) {
		Long an = asNumber(a);
		Long bn = asNumber(b);
		if(an != null && bn != null)
			return an.compareTo(bn);
		if(an != null)
			return -1;
		if(bn != null)
			return 1;
		return a.compareTo(b);
	}

	@Nullable
	private static Long asNumber(String s) {
		try {
			return Long.valueOf(s.trim());
		} catch(NumberFormatException x) {
			return null;
		}
	}

	public int getItemLevel() {
		return m_itemLevel;
	}

	public int getMenuIndex() {
		return m_menuIndex;
	}

	public void setMenuIndex(int menuIndex) {
		m_menuIndex = menuIndex;
	}

	public List<MenuItem> getSubItemList() {
		return m_subItemList;
	}

	public ContentLevel getLevel() {
		return m_level;
	}

	@Override
	public String toString() {
		return m_item.getRelativePath() + " [" + m_level.getRelativePath() + "] " + m_title;
	}
}

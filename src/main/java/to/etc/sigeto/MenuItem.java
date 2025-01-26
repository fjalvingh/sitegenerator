package to.etc.sigeto;

import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A single item in the generated menu.
 */
final public class MenuItem {
	private final ContentLevel m_level;

	private final ContentItem m_item;

	/** The title to use for the entry */
	private final String m_title;

	private final String m_sortTitle;

	private final int m_itemLevel;

	private int m_menuIndex;

	private List<MenuItem> m_subItemList = new ArrayList<>();

	@Nullable
	private MenuItem m_parent;

	public MenuItem(@Nullable MenuItem parent, ContentLevel level, ContentItem item, String title, String sortTitle, int itemLevel) {
		m_parent = parent;
		m_level = level;
		m_item = item;
		m_title = title;
		m_sortTitle = sortTitle;
		m_itemLevel = itemLevel;
		if(level == null || item == null || title == null || sortTitle == null) {
			throw new IllegalArgumentException("Arguments cannot be null");
		}
	}

	public ContentItem getItem() {
		return m_item;
	}

	public String getTitle() {
		return m_title;
	}

	public String getSortTitle() {
		return m_sortTitle;
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

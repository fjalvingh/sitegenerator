package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**\
 * The site menu, constructed from the content.
 */
public class Menu {
	private final MenuItem m_root;

	private final Map<ContentItem, MenuItem> m_itemMap = new HashMap<>();

	private Menu(MenuItem root) {
		m_root = root;
	}

	public MenuItem getRoot() {
		return m_root;
	}

	/**
	 * The items at the top level of the menu. These are always shown, whatever
	 * page is being rendered.
	 */
	@NonNull
	public List<MenuItem> getRootItemList() {
		return m_root.getSubItemList();
	}

	/**
	 * The menu item representing the specified page, if that page is in the
	 * menu at all (resources and hidden pages are not).
	 */
	@Nullable
	public MenuItem findItem(@Nullable ContentItem item) {
		return item == null ? null : m_itemMap.get(item);
	}

	static public Menu create(Content content) {
		ContentLevel rootLevel = content.getPageRootLevel();
		MenuItem rootItem = new MenuItem(null, rootLevel, rootLevel.getRootItem(), "root", null, 0);
		ContentLevel index = rootLevel.getSubLevelList().stream()
			.filter(a -> a.getName().startsWith("index"))
			.findFirst().orElse(null);

		Menu menu = new Menu(rootItem);
		menu.generateMenuLevel(rootItem, index == null ? rootLevel : index);
		return menu;
	}

	private void generateMenuLevel(MenuItem rootItem, ContentLevel level) {
		Set<ContentItem> subItems = new LinkedHashSet<>(level.getSubItems());
		subItems.remove(level.getRootItem());				// The level's own page is the menu item for this level, not a child of it
		for(ContentLevel subLevel : level.getSubLevelList()) {
			if(subLevel.hasMarkdown()) {
				ContentItem item = subLevel.getRootItem();
				if(item == null) {
					throw new MessageException("No item in sublevel " + subLevel.getRelativePath());
				}
				subItems.remove(item);
				createItemIf(rootItem, item, subLevel);
			}
		}
		for(ContentItem item : subItems) {
			if(item.getFileType() == ContentFileType.Markdown) {
				createItemIf(rootItem, item, null);
			}
		}

		//-- Now sort the items
		rootItem.getSubItemList().sort(MenuItem.BY_MENU_ORDER);
		List<MenuItem> subItemList = rootItem.getSubItemList();
		for(int i = 0; i < subItemList.size(); i++) {
			MenuItem item = subItemList.get(i);
			item.setMenuIndex(i);
		}
	}

	@Nullable
	private MenuItem createItemIf(MenuItem rootItem, ContentItem item, @Nullable ContentLevel level) {
		String name = level == null ? Util.getFilenameSansExtension(item.getName()) : level.getName();
		String title = defaultTitle(item, name);
		String sortKey = sortPrefix(name);				// A numeric name prefix is the default sort order

		Object o = item.getFrontMatter().get("menu");
		Map<String, Object> options;
		if(o != null) {
			if(! (o instanceof Map)) {
				throw new MessageException(item.getRelativePath() + ": frontmatter 'menu' item should be a map");
			}
			options = (Map<String, Object>) o;

		} else {
			options = new HashMap<>();
		}

		o = options.get("title");
		if(o != null) {
			title = o.toString();
		}

		o = options.get("sort");
		if(o != null) {
			sortKey = o.toString();						// Front matter wins over the name prefix
		}
		o = options.get("hidden");
		if(o != null)
			return null;

		MenuItem mi = new MenuItem(rootItem, level == null ? item.getLevel() : level, item, title, sortKey, rootItem.getItemLevel() + 1);
		rootItem.getSubItemList().add(mi);
		m_itemMap.put(item, mi);

		if(level != null && level.hasMarkdown()) {
			generateMenuLevel(mi, level);
		}

		return mi;
	}

	/**
	 * The title to show for a page: the page's own title, or - for a page
	 * without a title heading - the name of the thing it was made from, so
	 * that a missing heading does not break the entire menu. A numeric sort
	 * prefix on that name is not part of the title.
	 */
	@NonNull
	private static String defaultTitle(ContentItem item, String name) {
		String title = item.getPageTitle();
		if(null != title && !title.isBlank()) {
			return title;
		}
		return stripSortPrefix(name);
	}

	/**
	 * The sort order encoded in a directory or file name that starts with a
	 * number followed by a dash, like "20-using-components"; null for any other
	 * name.
	 */
	@Nullable
	static String sortPrefix(String name) {
		int ix = prefixEnd(name);
		return ix < 0 ? null : name.substring(0, ix);
	}

	/**
	 * The name without its numeric sort prefix, if it has one.
	 */
	@NonNull
	static String stripSortPrefix(String name) {
		int ix = prefixEnd(name);
		return ix < 0 ? name : name.substring(ix + 1);
	}

	/**
	 * The index of the dash ending the numeric prefix of the name, or -1 if the
	 * name does not start with "number-".
	 */
	private static int prefixEnd(String name) {
		int len = name.length();
		int ix = 0;
		while(ix < len && Character.isDigit(name.charAt(ix)))
			ix++;
		if(ix == 0 || ix >= len - 1 || name.charAt(ix) != '-')
			return -1;
		return ix;
	}
}

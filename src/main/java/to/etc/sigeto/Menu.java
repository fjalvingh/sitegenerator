package to.etc.sigeto;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

	static public Menu create(Content content) {
		ContentLevel rootLevel = content.getPageRootLevel();
		MenuItem rootItem = new MenuItem(null, rootLevel, rootLevel.getRootItem(), "root", "root", 0);
		ContentLevel index = rootLevel.getSubLevelList().stream()
			.filter(a -> a.getName().startsWith("index"))
			.findFirst().orElse(null);

		generateMenuLevel(rootItem, index == null ? rootLevel : index);

		return new Menu(rootItem);
	}

	private static void generateMenuLevel(MenuItem rootItem, ContentLevel level) {
		Set<ContentItem> subItems = new HashSet<>(level.getSubItems());
		for(ContentLevel subLevel : level.getSubLevelList()) {
			if(subLevel.hasMarkdown()) {
				ContentItem item = subLevel.getRootItem();
				if(item == null) {
					throw new IllegalStateException("No item in sublevel " + subLevel.getRelativePath());
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
		rootItem.getSubItemList().sort(Comparator.comparing(a -> a.getSortTitle()));
		List<MenuItem> subItemList = rootItem.getSubItemList();
		for(int i = 0; i < subItemList.size(); i++) {
			MenuItem item = subItemList.get(i);
			item.setMenuIndex(i);
		}
	}

	private static MenuItem createItemIf(MenuItem rootItem, ContentItem item, ContentLevel level) {
		String title = item.getPageTitle();
		String sortTitle = title;

		Object o = item.getFrontMatter().get("menu");
		Map<String, Object> options;
		if(o != null) {
			if(! (o instanceof Map)) {
				throw new IllegalStateException(item.getRelativePath() + ": frontmatter 'menu' item should be a map");
			}
			options = (Map<String, Object>) o;

		} else {
			options = new HashMap<>();
		}

		o = options.get("title");
		if(o != null) {
			title = o.toString();
			sortTitle = title;
		}

		o = options.get("sort");
		if(o != null) {
			sortTitle = o.toString();
		}
		o = options.get("hidden");
		if(o != null)
			return null;

		MenuItem mi = new MenuItem(rootItem, level == null ? item.getLevel() : level, item, title, sortTitle, rootItem.getItemLevel() + 1);
		rootItem.getSubItemList().add(mi);

		if(level != null && level.hasMarkdown()) {
			generateMenuLevel(mi, level);
		}

		return mi;
	}


}

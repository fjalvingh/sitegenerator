package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Content {
	/** The map of relative path to item */
	private Map<String, ContentItem> m_itemMap = new HashMap<String, ContentItem>();

	/** The paths in {@link #m_itemMap} indexed by their {@link #nameOf(String) name}, built when first asked for. */
	@Nullable
	private Map<String, List<String>> m_nameMap;

	private int m_markDownItemCount;

	/** For MarkDown items this is the rendering of the file, after all checks. */
	private String m_renderedContent;

	private Set<ContentItem> m_usedResourceList = new HashSet<>();

	private ContentLevel m_pageRootLevel;

	@Nullable
	private Menu m_menu;

	private final Map<String, ContentTag> m_tagMap = new HashMap<>();

	/** All blog entries in the whole content tree, sorted ascending by blog date - the sitewide global timeline. */
	private List<ContentLevel> m_allBlogEntries = new ArrayList<>();

	public static Content create(File root) {
		Content content = new Content();
		content.initialize(root);
		return content;
	}

	private void initialize(File root) {
		StringBuilder sb = new StringBuilder();
		ContentLevel rootLevel = scanContent(sb, null, root, ContentType.Page);
		if(null == rootLevel)
			throw new MessageException("No content inside the content directory");
		m_pageRootLevel = rootLevel;
	}

	public void complete() {
		//-- Create the site menu
		m_menu = Menu.create(this);

		//-- Build the sitewide global blog timeline
		List<ContentLevel> allBlogs = new ArrayList<>();
		collectBlogEntries(m_pageRootLevel, allBlogs);
		allBlogs.sort(Comparator.comparing(ContentLevel::getBlogDate));
		m_allBlogEntries = allBlogs;
	}

	private static void collectBlogEntries(ContentLevel level, List<ContentLevel> out) {
		out.addAll(level.getBlogEntryList());
		for(ContentLevel sub : level.getSubLevelList()) {
			collectBlogEntries(sub, out);
		}
	}

	/**
	 * All blog entries in the whole content tree, sorted ascending by blog date.
	 */
	public List<ContentLevel> getAllBlogEntries() {
		return m_allBlogEntries;
	}

	@Nullable
	public ContentLevel getPreviousGlobalBlog(ContentLevel entry) {
		int idx = m_allBlogEntries.indexOf(entry);
		return idx <= 0 ? null : m_allBlogEntries.get(idx - 1);
	}

	@Nullable
	public ContentLevel getNextGlobalBlog(ContentLevel entry) {
		int idx = m_allBlogEntries.indexOf(entry);
		return idx == -1 || idx == m_allBlogEntries.size() - 1 ? null : m_allBlogEntries.get(idx + 1);
	}

	@Nullable
	public static ContentLevel getPreviousLocalBlog(ContentLevel entry) {
		ContentLevel parent = entry.getParentLevel();
		if(null == parent)
			return null;
		List<ContentLevel> list = new ArrayList<>(parent.getBlogEntryList());
		list.sort(Comparator.comparing(ContentLevel::getBlogDate));
		int idx = list.indexOf(entry);
		return idx <= 0 ? null : list.get(idx - 1);
	}

	@Nullable
	public static ContentLevel getNextLocalBlog(ContentLevel entry) {
		ContentLevel parent = entry.getParentLevel();
		if(null == parent)
			return null;
		List<ContentLevel> list = new ArrayList<>(parent.getBlogEntryList());
		list.sort(Comparator.comparing(ContentLevel::getBlogDate));
		int idx = list.indexOf(entry);
		return idx == -1 || idx == list.size() - 1 ? null : list.get(idx + 1);
	}

	@Nullable
	private ContentLevel scanContent(StringBuilder sb, @Nullable ContentLevel parentLevel, File root, ContentType type) {
		int len = sb.length();
		String levelPath = sb.toString();
		ContentLevel level = new ContentLevel(root, levelPath, type, parentLevel);

		File[] files = root.listFiles();
		if(null == files || files.length == 0) {
			return null;
		}
		for(File file : files) {
			sb.setLength(len);
			if(len > 0)
				sb.append('/');
			sb.append(file.getName());
			if(file.isFile()) {
				String relative = sb.toString();
				ContentItem ci = new ContentItem(this, level, file, type, getType(file), relative);
				if(ci.getFileType() == ContentFileType.Markdown) {
					m_markDownItemCount++;
				}
				m_itemMap.put(relative, ci);
				level.addItem(ci);
			} else if(file.isDirectory()) {
				//-- Should not have these inside a blog dir
				if(type == ContentType.Blog)
					throw new MessageException("Unexpected directory " + file + " inside blog entry");

				if(isBlogDirectory(file)) {
					ContentLevel contentLevel = scanContent(sb, level, file, ContentType.Blog);
					if(null != contentLevel) {
						level.addBlogEntry(contentLevel);
					}
				} else {
					ContentLevel contentLevel = scanContent(sb, level, file, type);
					if(null != contentLevel) {
						level.addSubLevel(contentLevel);
					}
				}
			}
		}

		if(!level.hasMarkdown()) {
			System.out.println("No index page found for directory " + level);
		}

		return level.getSubItems().isEmpty() && level.getSubLevelList().isEmpty() && level.getBlogEntryList().isEmpty() ? null : level;
	}

	/**
	 * A blog directory has a numeric format, yyyymmdd. We consider a dir
	 * a blog dir if the parts are numeric and reasonable.
	 */
	static private boolean isBlogDirectory(File file) {
		String name = file.getName();
		if(name.length() != 8)
			return false;

		int year;
		try {
			year = Integer.parseInt(name.substring(0, 4));
		} catch(Exception x) {
			return false;
		}

		int month;
		try {
			month = Integer.parseInt(name.substring(4, 6));
		} catch(Exception x) {
			return false;
		}

		int day;
		try {
			day = Integer.parseInt(name.substring(6, 8));
		} catch(Exception x) {
			return false;
		}

		return year >= 2024
			&& year < 2100
			&& month >= 1
			&& month <= 12
			&& day >= 1
			&& day <= 31;
	}

	private ContentFileType getType(File file) {
		String name = file.getName();
		int pos = name.lastIndexOf(".");
		if(pos == -1) {
			return ContentFileType.Resource;
		}
		String ext = name.substring(pos + 1).toLowerCase();
		switch(ext){
			default:
				return ContentFileType.Resource;

			case "png":
			case "gif":
			case "jpg":
			case "jpeg":
				return ContentFileType.Image;

			case "md":
			case "mdown":
				return ContentFileType.Markdown;
		}
	}

	static public boolean isRelativePath(String url) {
		if(url.indexOf(':') != -1)                // http(s): url?
			return false;                        // We cannot check those currently
		if(url.startsWith("#"))
			return false;
		return true;
	}

	public ContentTag getTag(String tagName) {
		return m_tagMap.computeIfAbsent(tagName.toLowerCase(), a -> new ContentTag(a));
	}

	public List<ContentItem> getItemList() {
		return new ArrayList<ContentItem>(m_itemMap.values());
	}

	public int getMarkDownItemCount() {
		return m_markDownItemCount;
	}

	@Nullable
	public ContentItem findItem(String fullPath) {
		return m_itemMap.get(fullPath);
	}

	/**
	 * The content relative paths of everything known by the given name - the way
	 * a document is found back after it moved with nothing recording where to.
	 * Empty when the name is unused, and holding more than one path when the name
	 * is not unique, in which case nothing can be concluded from it.
	 */
	@NonNull
	public List<String> findPathsByName(@NonNull String name) {
		Map<String, List<String>> map = m_nameMap;
		if(null == map) {
			map = new HashMap<>();
			for(String path : m_itemMap.keySet()) {
				map.computeIfAbsent(nameOf(path), a -> new ArrayList<>()).add(path);
			}
			map.values().forEach(Collections::sort);					// The item map is unordered; reports should not be
			m_nameMap = map;
		}
		List<String> list = map.get(name);
		return null == list ? Collections.emptyList() : list;
	}

	/**
	 * The name a content relative path is known by: for a document living in a
	 * directory of its own ("data/qcriteria/index.md") that is the directory's
	 * name, since that is what the page is called and what travels with it when
	 * it is moved; for anything else it is the file name.
	 */
	@NonNull
	static String nameOf(@NonNull String path) {
		int slash = path.lastIndexOf('/');
		String name = path.substring(slash + 1);
		if(slash < 0 || !isIndexName(name))
			return name;
		int previous = path.lastIndexOf('/', slash - 1);
		return path.substring(previous + 1, slash);
	}

	private static boolean isIndexName(@NonNull String name) {
		int dot = name.lastIndexOf('.');
		return dot > 0 && "index".equalsIgnoreCase(name.substring(0, dot)) && MoveMap.isDocument(name);
	}

	public String getRenderedContent() {
		return m_renderedContent;
	}

	public void setRenderedContent(String renderedContent) {
		m_renderedContent = renderedContent;
	}

	void appendUsedResource(ContentItem item) {
		m_usedResourceList.add(item);
	}

	public Set<ContentItem> getUsedResourceList() {
		return m_usedResourceList;
	}

	@NonNull
	public ContentLevel getPageRootLevel() {
		return m_pageRootLevel;
	}

	public Menu getMenu() {
		return Objects.requireNonNull(m_menu);
	}
}

package to.etc.sigeto;

import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This contains a level of content, i.e. a subdirectory of items.
 */
final public class ContentLevel {
	private final File m_levelDirectory;

	private final String m_relativePath;

	private final ContentType m_contentType;

	/** Items directly at this sublevel */
	private final List<ContentItem> m_subItems = new ArrayList<>();

	private final List<ContentLevel> m_subLevelList = new ArrayList<>();

	private final List<ContentLevel> m_blogEntryList = new ArrayList<>();

	@Nullable
	private ContentItem m_rootItem;

	@Nullable
	private final ContentLevel m_parentLevel;

	private boolean m_hasMarkdown;

	/** For ContentType.Blog levels: the date parsed from the yyyymmdd directory name. */
	@Nullable
	private final LocalDate m_blogDate;

	/**
	 * True if this is a ContentType.Blog level whose immediate parent is the
	 * designated global blog root (the top-level "content/blogs" directory).
	 * Such entries have no "story" of their own, so they get a single rendered
	 * page using the sitewide global navigation, instead of the local+global
	 * pair used for blog entries nested inside a story.
	 */
	private final boolean m_globalBlogRoot;

	public ContentLevel(File levelDirectory, String relativePath, ContentType contentType, @Nullable ContentLevel parentLevel) {
		m_levelDirectory = levelDirectory;
		m_relativePath = relativePath;
		m_contentType = contentType;
		m_parentLevel = parentLevel;
		if(contentType == ContentType.Blog) {
			m_blogDate = parseBlogDate(levelDirectory.getName());
			m_globalBlogRoot = parentLevel != null
				&& "blogs".equalsIgnoreCase(parentLevel.getName())
				&& parentLevel.getParentLevel() != null
				&& parentLevel.getParentLevel().getParentLevel() == null;
		} else {
			m_blogDate = null;
			m_globalBlogRoot = false;
		}
	}

	@Nullable
	private static LocalDate parseBlogDate(String name) {
		if(name.length() != 8)
			return null;
		try {
			int year = Integer.parseInt(name.substring(0, 4));
			int month = Integer.parseInt(name.substring(4, 6));
			int day = Integer.parseInt(name.substring(6, 8));
			return LocalDate.of(year, month, day);
		} catch(Exception x) {
			return null;
		}
	}

	public String getName() {
		return m_levelDirectory.getName();
	}

	@Nullable
	public ContentItem findItemByName(String name) {
		for(ContentItem item : getSubItems()) {
			if(item.getName().equals(name)) {
				return item;
			}
		}
		return null;
	}

	public File getLevelDirectory() {
		return m_levelDirectory;
	}

	public String getRelativePath() {
		return m_relativePath;
	}

	public ContentType getContentType() {
		return m_contentType;
	}

	public List<ContentItem> getSubItems() {
		return m_subItems;
	}

	@Nullable
	public ContentItem getRootItem() {
		return m_rootItem;
	}

	@Nullable public ContentLevel getParentLevel() {
		return m_parentLevel;
	}

	public void addItem(ContentItem item) {
		if(item.getFileType() == ContentFileType.Markdown) {
			m_hasMarkdown = true;

			//-- Do we already have a md file? Then abort
			if(m_subItems.stream().anyMatch(a -> a.getFileType() == ContentFileType.Markdown)) {
				throw new MessageException(this + ": More than one markdown file found, this is not allowed");
			}
			m_rootItem = item;
		}
		m_subItems.add(item);
	}

	public void addSubLevel(ContentLevel level) {
		m_subLevelList.add(level);
	}

	public void addBlogEntry(ContentLevel level) {
		m_blogEntryList.add(level);
	}

	public boolean hasMarkdown() {
		return m_hasMarkdown;
	}

	/**
	 * Return T if this content level is a child or subchild of the specified
	 * level.
	 */
	public boolean isInside(ContentLevel level) {
		ContentLevel test = this;
		while(test != null) {
			if(test == level)
				return true;
			test = test.getParentLevel();
		}
		return false;
	}

	public List<ContentLevel> getBlogEntryList() {
		return m_blogEntryList;
	}

	public List<ContentLevel> getSubLevelList() {
		return m_subLevelList;
	}

	@Nullable
	public LocalDate getBlogDate() {
		return m_blogDate;
	}

	/**
	 * True for a ContentType.Blog level directly under the top-level
	 * "content/blogs" directory - such entries have no story of their own.
	 */
	public boolean isGlobalBlogRoot() {
		return m_globalBlogRoot;
	}

	/**
	 * The output-relative path of the page that represents this blog entry in
	 * the sitewide global timeline: its natural page for global-root entries,
	 * or a mirrored page under the "blog-timeline/" namespace for blog entries
	 * nested inside a story.
	 */
	@Nullable
	public String getGlobalOutputPath() {
		ContentItem rootItem = getRootItem();
		if(null == rootItem)
			return null;
		String natural = rootItem.getRelativeTargetPath();
		return isGlobalBlogRoot() ? natural : "blog-timeline/" + natural;
	}

	@Override public String toString() {
		return getRelativePath() + " [" + getContentType() + "]";
	}
}

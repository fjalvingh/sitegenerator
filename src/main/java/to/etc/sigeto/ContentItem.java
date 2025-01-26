package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContentItem {
	private final File m_file;

	/** T if this is the index item (the home page) */
	private final boolean m_indexItem;

	private String m_name;

	private final ContentType m_type;

	private final ContentFileType m_fileType;

	//private final String m_relativePath;

	private String m_pageTitle;

	@NonNull
	private ContentLevel m_level;

	/** Used content items, and each item has a list of relative paths used. */
	private Map<ContentItem, Set<String>> m_usedItemList = new HashMap<>();

	private final Map<String, Object> m_frontMatter = new HashMap<>();

	private final Map<String, ContentTag> m_tagMap = new HashMap<>();

	public ContentItem(@NonNull ContentLevel level, File file, ContentType type, ContentFileType fileType, String relativePath, boolean indexItem) {
		m_level = level;
		m_file = file;
		m_name = file.getName();
		m_type = type;
		m_fileType = fileType;
		m_indexItem = indexItem;
	}

	@NonNull
	public ContentLevel getLevel() {
		return m_level;
	}

	public void moveTo(@NonNull ContentLevel level, @NonNull String newName) {
		if(m_level != level) {
			m_level.getSubItems().remove(this);				// Remove from old level
			level.getSubItems().add(this);						// Add to new level
			m_level = level;
		}
		m_name = newName;
	}

	public String getName() {
		return m_name;
	}

	public void setLevel(@NonNull ContentLevel level) {
		m_level = level;
	}

	public File getFile() {
		return m_file;
	}

	public ContentType getType() {
		return m_type;
	}

	public ContentFileType getFileType() {
		return m_fileType;
	}

	public String getRelativePath() {
		String lp = m_level.getRelativePath();
		return lp.length() == 0 ? getName() : lp + "/" + getName();
	}

	public String getRelativeTargetPath() {
		if(getFileType() == ContentFileType.Markdown) {
			String lp = m_level.getRelativePath();
			String name = Util.getFilenameSansExtension(getName()) + ".html";

			return lp.length() == 0 ? name : lp + "/" + name;
		} else {
			return getRelativePath();
		}
	}

	public String getDirectoryPath() {
		return m_level.getRelativePath();
	}

	public String getPageTitle() {
		return m_pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		m_pageTitle = pageTitle;
	}

	public void addUsedItem(ContentItem item, String relativePath) {
		m_usedItemList.computeIfAbsent(item, a -> new HashSet<>()).add(relativePath);
	}

	public Map<ContentItem, Set<String>> getUsedItemList() {
		return m_usedItemList;
	}

	/**
	 * Return T if this is inside the level or a sublevel of that level.
	 */
	public boolean isInside(ContentLevel level) {
		return m_level.isInside(level);
	}

	@Override public String toString() {
		return getRelativePath() + " [" + m_fileType + "]";
	}

	public void setFrontMatter(Map<String, Object> map) {
		m_frontMatter.putAll(map);
	}

	public Map<String, Object> getFrontMatter() {
		return m_frontMatter;
	}

	public boolean isIndexItem() {
		return m_indexItem;
	}
}

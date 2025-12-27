package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContentItem {
	private final File m_file;

	private String m_name;

	private final ContentType m_type;

	private final ContentFileType m_fileType;

	//private final String m_relativePath;

	private String m_pageTitle;

	@NonNull private final Content m_content;

	@NonNull
	private ContentLevel m_level;

	/** Used content items, and each item has a list of relative paths used. */
	private Map<ContentItem, Set<String>> m_usedItemList = new HashMap<>();

	private final Map<String, Object> m_frontMatter = new HashMap<>();

	private final Map<String, ContentTag> m_tagMap = new HashMap<>();

	public ContentItem(@NonNull Content content, @NonNull ContentLevel level, File file, ContentType type, ContentFileType fileType, String relativePath) {
		m_content = content;
		m_level = level;
		m_file = file;
		m_name = file.getName();
		m_type = type;
		m_fileType = fileType;
	}

	@NonNull
	public ContentLevel getLevel() {
		return m_level;
	}

	@NonNull public Content getContent() {
		return m_content;
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

	@Nullable
	public ContentItem findItemByURL(String url) {
		if(!Content.isRelativePath(url))
			return null;

		String fullPath;
		if(url.startsWith("/")) {
			fullPath = url.substring(1);
		} else {
			//-- Relative wrt the parent
			Path path = Path.of(getDirectoryPath());
			Path resolvedPath = path.resolve(url).normalize();
			fullPath = resolvedPath.toString();
		}

		ContentItem item = getContent().findItem(fullPath);
		return item;
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
}

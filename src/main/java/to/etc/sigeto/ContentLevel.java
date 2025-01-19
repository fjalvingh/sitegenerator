package to.etc.sigeto;

import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
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

	private ContentItem m_rootItem;

	@Nullable
	private final ContentLevel m_parentLevel;

	public ContentLevel(File levelDirectory, String relativePath, ContentType contentType, @Nullable ContentLevel parentLevel) {
		m_levelDirectory = levelDirectory;
		m_relativePath = relativePath;
		m_contentType = contentType;
		m_parentLevel = parentLevel;
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

	public ContentItem getRootItem() {
		return m_rootItem;
	}

	public void setRootItem(ContentItem rootItem) {
		m_rootItem = rootItem;
	}

	@Nullable public ContentLevel getParentLevel() {
		return m_parentLevel;
	}

	public void addItem(ContentItem item) {
		m_subItems.add(item);
	}

	public void addSubLevel(ContentLevel level) {
		m_subLevelList.add(level);
	}

	public List<ContentLevel> getSubLevelList() {
		return m_subLevelList;
	}
}

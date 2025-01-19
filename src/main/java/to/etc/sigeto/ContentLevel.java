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

	private boolean m_hasMarkdown;

	public ContentLevel(File levelDirectory, String relativePath, ContentType contentType, @Nullable ContentLevel parentLevel) {
		m_levelDirectory = levelDirectory;
		m_relativePath = relativePath;
		m_contentType = contentType;
		m_parentLevel = parentLevel;
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
		if(item.getFileType() == ContentFileType.Markdown)
			m_hasMarkdown = true;
	}

	public void addSubLevel(ContentLevel level) {
		m_subLevelList.add(level);
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

	public List<ContentLevel> getSubLevelList() {
		return m_subLevelList;
	}

	@Override public String toString() {
		return getRelativePath() + " [" + getContentType() + "]";
	}
}

package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContentItem {
	private final File m_file;

	private final ContentType m_type;

	private final ContentFileType m_fileType;

	private final String m_relativePath;

	private String m_pageTitle;

	@NonNull
	private ContentLevel m_level;

	private List<ContentItem> m_usedItemList = new ArrayList<>();

	public ContentItem(@NonNull ContentLevel level, File file, ContentType type, ContentFileType fileType, String relativePath) {
		m_level = level;
		m_file = file;
		m_type = type;
		m_fileType = fileType;
		m_relativePath = relativePath;
	}

	@NonNull
	public ContentLevel getLevel() {
		return m_level;
	}

	public String getName() {
		return m_file.getName();
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
		return m_relativePath;
	}

	public String getDirectoryPath() {
		int pos = m_relativePath.lastIndexOf("/");
		if(pos == -1)
			return "";
		return m_relativePath.substring(0, pos + 1);
	}

	public String getPageTitle() {
		return m_pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		m_pageTitle = pageTitle;
	}

	public void addUsedItem(ContentItem item) {
		m_usedItemList.add(item);
	}

	public List<ContentItem> getUsedItemList() {
		return m_usedItemList;
	}

	@Override public String toString() {
		return m_relativePath + " [" + m_fileType + "]";
	}
}

package to.etc.sigeto;

import java.io.File;

public class ContentItem {
	private final File m_file;

	private final ContentType m_type;

	private final ContentFileType m_fileType;

	private final String m_relativePath;

	private String m_pageTitle;

	public ContentItem(File file, ContentType type, ContentFileType fileType, String relativePath) {
		m_file = file;
		m_type = type;
		m_fileType = fileType;
		m_relativePath = relativePath;
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

	@Override public String toString() {
		return m_relativePath + " [" + m_fileType + "]";
	}
}

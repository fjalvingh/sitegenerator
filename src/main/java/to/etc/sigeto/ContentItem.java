package to.etc.sigeto;

import java.io.File;

public class ContentItem {
	private final File m_file;

	private final ContentType m_type;

	private final String m_relativePath;

	public ContentItem(File file, ContentType type, String relativePath) {
		m_file = file;
		m_type = type;
		m_relativePath = relativePath;
	}

	public File getFile() {
		return m_file;
	}

	public ContentType getType() {
		return m_type;
	}

	public String getRelativePath() {
		return m_relativePath;
	}
}

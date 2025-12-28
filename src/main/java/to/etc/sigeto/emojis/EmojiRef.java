package to.etc.sigeto.emojis;

final class EmojiRef {
	private final String m_root;

	private final String m_githubRef;

	public EmojiRef(String root, String githubRef) {
		m_root = root;
		m_githubRef = githubRef;
	}

	public String getUrl() {
		return m_root + m_githubRef;
	}
}

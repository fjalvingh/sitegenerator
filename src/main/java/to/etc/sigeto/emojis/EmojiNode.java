package to.etc.sigeto.emojis;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Delimited;

final public class EmojiNode extends CustomNode implements Delimited {
	private final String m_delimiter;

	private EmojiRef m_ref;

	public EmojiNode(String delimiter, EmojiRef ref) {
		m_delimiter = delimiter;
		m_ref = ref;
	}

	@Override
	public String getOpeningDelimiter() {
		return m_delimiter;
	}

	@Override public String getClosingDelimiter() {
		return m_delimiter;
	}

	public EmojiRef getRef() {
		return m_ref;
	}
}

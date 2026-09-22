package to.etc.sigeto.subsup;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Delimited;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A run of inline content that was wrapped in "~" (subscript) or "^"
 * (superscript) delimiters; the children are the wrapped content.
 */
final public class SubSupNode extends CustomNode implements Delimited {
	@NonNull
	private final String m_delimiter;

	/** The html tag this renders as: "sub" or "sup". */
	@NonNull
	private final String m_tagName;

	public SubSupNode(@NonNull String delimiter, @NonNull String tagName) {
		m_delimiter = delimiter;
		m_tagName = tagName;
	}

	@Override
	@NonNull
	public String getOpeningDelimiter() {
		return m_delimiter;
	}

	@Override
	@NonNull
	public String getClosingDelimiter() {
		return m_delimiter;
	}

	@NonNull
	public String getTagName() {
		return m_tagName;
	}
}

package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A link or image in a markdown source that points at a document which has
 * moved, plus the destination it should point at instead. Collected by
 * {@link MarkdownChecker} and applied to the source files by
 * {@link SourceLinkFixer}.
 */
public class LinkFix {
	@NonNull
	private final ContentItem m_item;

	/** The 1-based line in the source file the link was found on, or 0 when unknown. */
	private final int m_lineNumber;

	@NonNull
	private final String m_oldUrl;

	@NonNull
	private final String m_newUrl;

	public LinkFix(@NonNull ContentItem item, int lineNumber, @NonNull String oldUrl, @NonNull String newUrl) {
		m_item = item;
		m_lineNumber = lineNumber;
		m_oldUrl = oldUrl;
		m_newUrl = newUrl;
	}

	@NonNull
	public ContentItem getItem() {
		return m_item;
	}

	public int getLineNumber() {
		return m_lineNumber;
	}

	@NonNull
	public String getOldUrl() {
		return m_oldUrl;
	}

	@NonNull
	public String getNewUrl() {
		return m_newUrl;
	}

	@Override public String toString() {
		return m_item.getRelativePath() + "(" + m_lineNumber + "): " + m_oldUrl + " -> " + m_newUrl;
	}
}

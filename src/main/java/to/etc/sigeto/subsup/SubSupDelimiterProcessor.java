package to.etc.sigeto.subsup;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Node;
import org.commonmark.node.Nodes;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.SourceSpans;
import org.commonmark.node.Text;
import org.commonmark.parser.delimiter.DelimiterProcessor;
import org.commonmark.parser.delimiter.DelimiterRun;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Recognizes "~sub~" and "^sup^" and wraps what is between the delimiters in a
 * {@link SubSupNode}.
 *
 * <p>Like Pandoc's version of this syntax the content may not contain
 * whitespace: a "~" or "^" is far more often a tilde or a caret in running
 * text ("under ~/opt", "stop with ^C") than it is a delimiter, and demanding
 * that the two markers sit around a single word is what keeps those from
 * silently turning into a subscript. Content with a space in it is left alone,
 * delimiters and all.</p>
 */
final public class SubSupDelimiterProcessor implements DelimiterProcessor {
	private final char m_character;

	@NonNull
	private final String m_tagName;

	public SubSupDelimiterProcessor(char character, @NonNull String tagName) {
		m_character = character;
		m_tagName = tagName;
	}

	@Override
	public char getOpeningCharacter() {
		return m_character;
	}

	@Override
	public char getClosingCharacter() {
		return m_character;
	}

	@Override
	public int getMinLength() {
		return 1;
	}

	@Override
	public int process(DelimiterRun openingRun, DelimiterRun closingRun) {
		if(openingRun.length() != 1 || closingRun.length() != 1) {		// Only ~x~, never ~~x~~
			return 0;
		}

		Text opener = openingRun.getOpener();
		if(! isUsableContent(opener, closingRun.getCloser())) {
			return 0;
		}

		SubSupNode subSup = new SubSupNode(String.valueOf(m_character), m_tagName);

		SourceSpans sourceSpans = new SourceSpans();
		sourceSpans.addAllFrom(openingRun.getOpeners(1));

		for(Node node : Nodes.between(opener, closingRun.getCloser())) {
			subSup.appendChild(node);
			sourceSpans.addAll(node.getSourceSpans());
		}

		sourceSpans.addAllFrom(closingRun.getClosers(1));
		subSup.setSourceSpans(sourceSpans.getSourceSpans());

		opener.insertAfter(subSup);
		return 1;
	}

	/**
	 * T when there is something between the delimiters and no part of it holds
	 * whitespace.
	 */
	private static boolean isUsableContent(@NonNull Node opener, @NonNull Node closer) {
		WhitespaceVisitor visitor = new WhitespaceVisitor();
		boolean empty = true;
		for(Node node : Nodes.between(opener, closer)) {
			empty = false;
			node.accept(visitor);
		}
		return !empty && !visitor.isWhitespaceSeen();
	}

	/** Reports whether any of the inline content it visited holds whitespace. */
	private static final class WhitespaceVisitor extends AbstractVisitor {
		private boolean m_whitespaceSeen;

		@Override
		public void visit(Text text) {
			check(text.getLiteral());
		}

		@Override
		public void visit(Code code) {
			check(code.getLiteral());
		}

		@Override
		public void visit(SoftLineBreak softLineBreak) {
			m_whitespaceSeen = true;
		}

		@Override
		public void visit(HardLineBreak hardLineBreak) {
			m_whitespaceSeen = true;
		}

		private void check(@NonNull String literal) {
			for(int i = 0; i < literal.length(); i++) {
				if(Character.isWhitespace(literal.charAt(i))) {
					m_whitespaceSeen = true;
					return;
				}
			}
		}

		public boolean isWhitespaceSeen() {
			return m_whitespaceSeen;
		}
	}
}

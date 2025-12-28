package to.etc.sigeto.emojis;

import org.commonmark.node.Node;
import org.commonmark.node.Nodes;
import org.commonmark.node.SourceSpans;
import org.commonmark.node.Text;
import org.commonmark.parser.delimiter.DelimiterProcessor;
import org.commonmark.parser.delimiter.DelimiterRun;

public class EmojiDelimiterProcessor implements DelimiterProcessor {
	@Override public char getOpeningCharacter() {
		return ':';
	}

	@Override public char getClosingCharacter() {
		return ':';
	}

	@Override public int getMinLength() {
		return 1;
	}

	@Override public int process(DelimiterRun openingRun, DelimiterRun closingRun) {
		if(openingRun.length() != closingRun.length() || openingRun.length() != 1) {			// Only :xxx:
			return 0;
		}

		Text opener = openingRun.getOpener();

		// Wrap nodes between delimiters in strikethrough.
		String delimiter = openingRun.length() == 1 ? opener.getLiteral() : opener.getLiteral() + opener.getLiteral();
		StringBuilder sb = new StringBuilder();
		for(Node node : Nodes.between(opener, closingRun.getCloser())) {
			if(node instanceof Text txt) {
				sb.append(txt.getLiteral());
			}
		}

		EmojiRef ref = EmojiRegistry.find(sb.toString());
		if(null == ref)
			return 0;

		Node emojiNode = new EmojiNode(delimiter, ref);

		SourceSpans sourceSpans = new SourceSpans();
		sourceSpans.addAllFrom(openingRun.getOpeners(openingRun.length()));

		for(Node node : Nodes.between(opener, closingRun.getCloser())) {
			emojiNode.appendChild(node);
			sourceSpans.addAll(node.getSourceSpans());
		}

		sourceSpans.addAllFrom(closingRun.getClosers(closingRun.length()));
		emojiNode.setSourceSpans(sourceSpans.getSourceSpans());

		opener.insertAfter(emojiNode);

		return openingRun.length();
	}
}

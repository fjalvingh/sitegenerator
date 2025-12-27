package to.etc.sigeto.tocextension;

import org.commonmark.node.Block;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockParserFactory;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

import java.util.HashSet;
import java.util.Set;

final public class TocParser extends AbstractBlockParser {
	private final Toc m_block;

	public TocParser(Set<String> options) {
		m_block = new Toc(options);
	}

	@Override public Block getBlock() {
		return m_block;
	}

	@Override public BlockContinue tryContinue(ParserState parserState) {
		return BlockContinue.none();
		//return BlockContinue.atIndex(parserState.getIndex());
	}

	/**
	 * Recognizes the [TOC xxxx] sequence.
	 */
	public static class Factory implements BlockParserFactory {
		@Override
		public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
			if(state.getIndent() >= 4) {
				return BlockStart.none();
			}
			var index = state.getNextNonSpaceIndex();
			var content = state.getLine().getContent();
			if(!isToc(content, index)) {
				return BlockStart.none();
			}

			//-- Scan everything until closing ].
			StringBuilder sb = new StringBuilder();
			index += TOC.length();
			Set<String> options = new HashSet<>();
			while(index < content.length()) {
				char c = Character.toLowerCase(content.charAt(index++));
				if(c == ']') {
					if(sb.length() > 0)
						options.add(sb.toString());
					return BlockStart.of(new TocParser(options)).atIndex(index);
				} else if(Character.isWhitespace(c)) {
					if(sb.length() > 0) {
						options.add(sb.toString());
						sb.setLength(0);
					}
				} else {
					sb.append(c);
				}
			}

			//-- Missing ]; ignore
			return BlockStart.none();
		}

		static private final String TOC = "[toc";

		private boolean isToc(CharSequence content, int index) {
			if(index + 3 >= content.length())
				return false;
			for(int i = 0; i < TOC.length(); i++) {
				char c = Character.toLowerCase(content.charAt(index + i));
				if(c != TOC.charAt(i))
					return false;
			}

			//-- Found "toc"
			return true;
		}
	}

}

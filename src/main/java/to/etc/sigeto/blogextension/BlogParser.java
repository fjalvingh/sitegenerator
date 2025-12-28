package to.etc.sigeto.blogextension;

import org.commonmark.node.Block;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockParserFactory;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

import java.util.HashSet;
import java.util.Set;

final public class BlogParser extends AbstractBlockParser {
	private final BlogNode m_block;

	public BlogParser() {
		m_block = new BlogNode();
	}

	@Override public Block getBlock() {
		return m_block;
	}

	@Override public BlockContinue tryContinue(ParserState parserState) {
		return BlockContinue.none();
	}

	/**
	 * Recognizes the [BLOG] sequence.
	 */
	public static class Factory implements BlockParserFactory {
		@Override
		public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
			if(state.getIndent() >= 4) {
				return BlockStart.none();
			}
			var index = state.getNextNonSpaceIndex();
			var content = state.getLine().getContent();
			if(!isBlog(content, index)) {
				return BlockStart.none();
			}

			//-- Scan everything until closing ].
			StringBuilder sb = new StringBuilder();
			index += BLOG.length();
			Set<String> options = new HashSet<>();
			while(index < content.length()) {
				char c = Character.toLowerCase(content.charAt(index++));
				if(c == ']') {
					if(sb.length() > 0)
						options.add(sb.toString());
					return BlockStart.of(new BlogParser()).atIndex(index);
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

		static private final String BLOG = "[blog";

		private boolean isBlog(CharSequence content, int index) {
			if(index + BLOG.length() >= content.length())
				return false;
			for(int i = 0; i < BLOG.length(); i++) {
				char c = Character.toLowerCase(content.charAt(index + i));
				if(c != BLOG.charAt(i))
					return false;
			}

			//-- Found
			return true;
		}
	}

}

package to.etc.sigeto.emojis;

import org.commonmark.node.Block;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockParserFactory;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

final class EmojiParser extends AbstractBlockParser {
	private final EmojiNode m_block;

	public EmojiParser(EmojiRef ref) {
		m_block = new EmojiNode(ref);
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public Block getBlock() {
		return m_block;
	}

	@Override
	public BlockContinue tryContinue(ParserState state) {
		return BlockContinue.none();
	}

	/**
	 * Recognize :xxx: as an emoji reference.
	 */
	public static class Factory implements BlockParserFactory {
		@Override
		public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
			if(state.getIndent() >= 4) {
				return BlockStart.none();
			}
			var index = state.getNextNonSpaceIndex();
			var content = state.getLine().getContent();
			if(index >= content.length() || content.charAt(index) != ':') {
				return BlockStart.none();
			}

			index++;
			StringBuilder sb = new StringBuilder();
			while(index < content.length()) {
				char c = content.charAt(index++);
				if(c == ':') {
					//-- Found emoji ref
					EmojiRef ref = EmojiRegistry.find(sb.toString());
					if(null == ref) {
						return BlockStart.none();
					}
					return BlockStart.of(new EmojiParser(ref)).atIndex(index);
				} else {
					sb.append(c);
				}
			}
			return BlockStart.none();
		}
	}
}

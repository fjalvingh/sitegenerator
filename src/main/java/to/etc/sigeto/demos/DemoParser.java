package to.etc.sigeto.demos;

import org.commonmark.node.Block;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockParserFactory;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes a line that is nothing but a "!demo(path)" tag, optionally
 * followed by the size to show the application in:
 *
 * <pre>
 *   !demo(to.etc.domuidemo.pages.HomePage.ui)
 *   !demo(to.etc.domuidemo.pages.HomePage.ui, 1024, 640)
 *   !demo(to.etc.domuidemo.pages.HomePage.ui, 100%)
 * </pre>
 *
 * The size is the width and then the height; leaving one out keeps its
 * default. The tag is a block: it must be on a line of its own.
 */
final public class DemoParser extends AbstractBlockParser {
	/** "!demo(" path ["," width ["," height]] ")", alone on its line. */
	private static final Pattern DEMO_LINE = Pattern.compile("!demo\\(([^,()]+?)\\s*(?:,\\s*([^,()]*?)\\s*)?(?:,\\s*([^,()]*?)\\s*)?\\)\\s*", Pattern.CASE_INSENSITIVE);

	private final DemoBlock m_block;

	public DemoParser(DemoBlock block) {
		m_block = block;
	}

	@Override
	public Block getBlock() {
		return m_block;
	}

	@Override
	public BlockContinue tryContinue(ParserState parserState) {
		return BlockContinue.none();								// One line, always
	}

	public static class Factory implements BlockParserFactory {
		@Override
		public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
			if(state.getIndent() >= 4) {							// Indented that far it is code, not a tag
				return BlockStart.none();
			}
			int index = state.getNextNonSpaceIndex();
			CharSequence line = state.getLine().getContent();
			Matcher matcher = DEMO_LINE.matcher(line.subSequence(index, line.length()));
			if(!matcher.matches()) {
				return BlockStart.none();
			}

			String path = matcher.group(1).trim();
			if(path.isEmpty()) {
				return BlockStart.none();							// "!demo()" is not a tag at all
			}
			String width = emptyAs(matcher.group(2), DemoBlock.DEFAULT_WIDTH);
			String height = emptyAs(matcher.group(3), DemoBlock.DEFAULT_HEIGHT);
			return BlockStart.of(new DemoParser(new DemoBlock(path, width, height))).atIndex(line.length());
		}

		private static String emptyAs(String value, String defaultValue) {
			return value == null || value.isBlank() ? defaultValue : value.trim();
		}
	}
}

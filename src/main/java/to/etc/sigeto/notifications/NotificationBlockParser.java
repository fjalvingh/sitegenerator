package to.etc.sigeto.notifications;

import org.commonmark.node.Block;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.AbstractBlockParserFactory;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationBlockParser extends AbstractBlockParser {
	private static final Pattern NOTIFICATIONS_LINE = Pattern.compile("\\s*!([v!xeiw]?)\\s(.*)");

	private final NotificationBlock m_block;

	private Notification m_type;

	public NotificationBlockParser(Notification type) {
		this.m_type = type;
		this.m_block = new NotificationBlock(type);
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public boolean canContain(Block block) {
		return block != null && !NotificationBlock.class.isAssignableFrom(block.getClass());
	}

	@Override
	public Block getBlock() {
		return m_block;
	}

	@Override
	public BlockContinue tryContinue(ParserState state) {
		CharSequence fullLine = state.getLine().getContent();
		CharSequence currentLine = fullLine.subSequence(state.getColumn() + state.getIndent(), fullLine.length());

		Matcher matcher = NOTIFICATIONS_LINE.matcher(currentLine);
		if(matcher.matches()) {
			if(m_type.equals(Notification.fromString(matcher.group(1)))) {
				return BlockContinue.atColumn(state.getColumn() + state.getIndent() + matcher.start(2));
			}
		}

		return BlockContinue.none();
	}

	public static class Factory extends AbstractBlockParserFactory {

		@Override
		public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
			CharSequence fullLine = state.getLine().getContent();
			CharSequence line = fullLine.subSequence(state.getColumn(), fullLine.length());
			Matcher matcher = NOTIFICATIONS_LINE.matcher(line);
			if(matcher.matches()) {
				return BlockStart
					.of(new NotificationBlockParser(Notification.fromString(matcher.group(1))))
					.atColumn(state.getColumn() + state.getIndent() + matcher.start(2));
			}
			return BlockStart.none();
		}
	}
}

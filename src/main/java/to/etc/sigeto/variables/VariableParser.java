package to.etc.sigeto.variables;

import org.commonmark.parser.beta.InlineContentParser;
import org.commonmark.parser.beta.InlineContentParserFactory;
import org.commonmark.parser.beta.InlineParserState;
import org.commonmark.parser.beta.ParsedInline;
import org.commonmark.parser.beta.Position;
import org.commonmark.parser.beta.Scanner;

import java.util.Set;

/**
 * Recognizes "${name}" in the text of a document. Doing this as part of the
 * inline parse means the usual markdown rules apply for free: a "${name}"
 * inside a code span or a code block is left alone, and "\${name}" is the
 * escape for a "${" that is meant literally.
 */
final public class VariableParser implements InlineContentParser {
	@Override
	public ParsedInline tryParse(InlineParserState state) {
		Scanner scanner = state.scanner();
		scanner.next();												// The '$' that got us here
		if(!scanner.next('{'))
			return ParsedInline.none();

		Position start = scanner.position();
		if(scanner.find('}') < 0)
			return ParsedInline.none();
		String name = scanner.getSource(start, scanner.position()).getContent();
		if(!Variables.isValidName(name))
			return ParsedInline.none();								// Not a variable: text that just happens to start with "${"
		scanner.next();												// The '}'
		return ParsedInline.of(new VariableNode(name), scanner.position());
	}

	public static class Factory implements InlineContentParserFactory {
		@Override
		public Set<Character> getTriggerCharacters() {
			return Set.of('$');
		}

		@Override
		public InlineContentParser create() {
			return new VariableParser();
		}
	}
}

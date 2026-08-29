package to.etc.sigeto.variables;

import org.eclipse.jdt.annotation.NonNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The "${name}" variable syntax: what a name is allowed to look like, and the
 * expansion of the variables inside a plain string. Strings are what link and
 * image urls are - they are not part of the inline parse, see
 * {@link VariableExpander}. Variables used in the text of a document are nodes
 * in the parsed document instead, see {@link VariableParser}.
 */
final public class Variables {
	private Variables() {
	}

	/**
	 * Variable names are deliberately restricted to letters, digits, '.', '-'
	 * and '_': it keeps a "${" that is meant literally from being taken for
	 * the start of a variable, and it keeps a name from running over the end
	 * of its line.
	 */
	public static boolean isValidName(@NonNull String name) {
		if(name.isEmpty())
			return false;
		for(int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if(!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_')
				return false;
		}
		return true;
	}

	/**
	 * Replace every "${name}" in the text with its value, verbatim. A name the
	 * resolver does not know is left exactly as it is written and reported
	 * through unknownConsumer, so the caller can complain about it with a file
	 * and a line number instead of silently leaving a hole in the page.
	 */
	@NonNull
	public static String expand(@NonNull String text, @NonNull Function<String, String> resolver, @NonNull Consumer<String> unknownConsumer) {
		int pos = text.indexOf("${");
		if(pos < 0)													// By far the usual case: nothing to do at all
			return text;

		StringBuilder sb = new StringBuilder(text.length() + 64);
		int done = 0;
		while(pos >= 0) {
			int end = text.indexOf('}', pos + 2);
			if(end < 0)												// An unclosed "${" is just text
				break;
			String name = text.substring(pos + 2, end);
			if(isValidName(name)) {
				sb.append(text, done, pos);
				String value = resolver.apply(name);
				if(null == value) {
					sb.append(text, pos, end + 1);					// Leave it as written; it is reported as an error
					unknownConsumer.accept(name);
				} else {
					sb.append(value);
				}
				done = end + 1;
			}
			pos = text.indexOf("${", pos + 2);
		}
		sb.append(text, done, text.length());
		return sb.toString();
	}
}

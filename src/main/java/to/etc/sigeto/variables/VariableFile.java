package to.etc.sigeto.variables;

import org.eclipse.jdt.annotation.NonNull;
import to.etc.sigeto.MessageException;
import to.etc.sigeto.Util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The site's own variable definitions: "variables.properties" in the site root,
 * next to content/ and templates/ - deliberately not inside content/, which
 * would copy it into the generated site.
 *
 * What the "${name}" variables in the documentation stand for is a property of
 * the site, not of whoever happens to run the generator: a build that forgets a
 * definition fails, so the definitions belong in a file that is committed with
 * the site instead of in an option every run has to repeat. The command line
 * can still override any of them with "-Dname=value", which is what a build
 * against another installation of the application needs.
 *
 * The syntax is one "name=value" per line, "#" starts a comment line, and blank
 * lines are ignored. The value is the rest of the line, trimmed, taken exactly
 * as it is written: there are no escapes and no continuation lines, so a url or
 * a Windows path needs no quoting. The file is read as UTF-8 - which is where
 * this differs from what {@link java.util.Properties} would do with it.
 */
final public class VariableFile {
	/** The name of the file, inside the site root. */
	public static final String FILENAME = "variables.properties";

	private VariableFile() {
	}

	/**
	 * Load the site's variables; a missing file just yields an empty map, as a
	 * site that uses no variables needs no file.
	 */
	@NonNull
	public static Map<String, String> load(@NonNull File siteRoot) throws Exception {
		Map<String, String> map = new LinkedHashMap<>();
		File file = new File(siteRoot, FILENAME);
		if(!file.exists())
			return map;

		int lineNumber = 0;
		for(String line : Util.readFileAsString(file).split("\n")) {
			lineNumber++;
			String trimmed = line.trim();
			if(trimmed.isEmpty() || trimmed.startsWith("#"))
				continue;
			int eq = trimmed.indexOf('=');
			if(eq < 0)
				throw new MessageException(file + "(" + lineNumber + "): expected 'name=value' but got: " + line);
			String name = trimmed.substring(0, eq).trim();
			String value = trimmed.substring(eq + 1).trim();
			if(!Variables.isValidName(name))
				throw new MessageException(file + "(" + lineNumber + "): '" + name + "' is not a variable name - it can only contain letters, digits, '.', '-' and '_'");
			if(null != map.put(name, value))
				throw new MessageException(file + "(" + lineNumber + "): '" + name + "' is defined twice");
		}
		return map;
	}
}

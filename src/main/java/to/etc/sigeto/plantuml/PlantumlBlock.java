package to.etc.sigeto.plantuml;

import org.commonmark.node.CustomBlock;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A fenced code block whose info string starts with "plantuml": the diagram
 * source, which the generator renders to an image file and embeds as an
 * &lt;img&gt; instead of showing it as code.
 *
 * <pre>
 *   ```plantuml
 *   Alice -&gt; Bob: hello
 *   ```
 *
 *   ```plantuml png title="The login handshake"
 *   &#64;startuml
 *   Alice -&gt; Bob: hello
 *   &#64;enduml
 *   ```
 * </pre>
 *
 * The "&#64;startuml" and "&#64;enduml" lines can be left out - the fence already says
 * where the diagram starts and ends, and PlantUML gets them added back before
 * it sees the source. See {@link #parseInfo} for what may follow "plantuml".
 */
public class PlantumlBlock extends CustomBlock {
	/** The word in the fence's info string that makes it a diagram. */
	public static final String INFO_WORD = "plantuml";

	/** The widest an embedded diagram is shown, in pixels; a wider one is scaled down. */
	public static final int MAX_WIDTH = 900;

	/** What the alt text says when the block did not give a title. */
	public static final String DEFAULT_TITLE = "PlantUML diagram";

	@NonNull
	private final String m_source;

	@NonNull
	private final PlantumlFormat m_format;

	/** The description the block gave, null when it did not give one. */
	@Nullable
	private final String m_title;

	/**
	 * The number of lines added in front of the source before handing it to
	 * PlantUML, needed to report an error on the line the author wrote it on.
	 */
	private final int m_prefixLines;

	/** The complaint about the fence's info string, or null when it was fine. */
	@Nullable
	private final String m_optionError;

	private PlantumlBlock(@NonNull String source, @NonNull PlantumlFormat format, @Nullable String title, int prefixLines, @Nullable String optionError) {
		m_source = source;
		m_format = format;
		m_title = title;
		m_prefixLines = prefixLines;
		m_optionError = optionError;
	}

	/**
	 * The block for a fenced code block that said "plantuml": its literal
	 * content is the diagram, its info string the options.
	 */
	@NonNull
	public static PlantumlBlock create(@NonNull String info, @NonNull String literal) {
		Options options = parseInfo(info);
		String error = options.error;
		if(null == error && literal.isBlank())
			error = INFO_WORD + ": the block is empty, there is no diagram to generate";
		boolean wrap = !literal.stripLeading().startsWith("@start");
		String source = wrap ? "@startuml\n" + literal + "@enduml\n" : literal;
		return new PlantumlBlock(source, options.format, options.title, wrap ? 1 : 0, error);
	}

	/**
	 * Whether this fence's info string asks for a diagram: the word "plantuml",
	 * by itself or followed by options.
	 */
	public static boolean isPlantuml(@Nullable String info) {
		if(null == info)
			return false;
		String text = info.trim();
		if(!text.regionMatches(true, 0, INFO_WORD, 0, INFO_WORD.length()))
			return false;
		return text.length() == INFO_WORD.length() || Character.isWhitespace(text.charAt(INFO_WORD.length()));
	}

	/**
	 * The diagram source as PlantUML gets it: what the block contains, with
	 * the "&#64;startuml"/"&#64;enduml" lines added when they were left out.
	 */
	@NonNull
	public String getSource() {
		return m_source;
	}

	@NonNull
	public PlantumlFormat getFormat() {
		return m_format;
	}

	/**
	 * What the diagram is about, as the block said it - null when it did not,
	 * in which case the image gets no title attribute.
	 */
	@Nullable
	public String getTitle() {
		return m_title;
	}

	/** The alt text of the generated image, which every image needs to have. */
	@NonNull
	public String getAltText() {
		String title = m_title;
		return null == title ? DEFAULT_TITLE : title;
	}

	public int getPrefixLines() {
		return m_prefixLines;
	}

	@Nullable
	public String getOptionError() {
		return m_optionError;
	}

	/**
	 * What may follow "plantuml" in the fence's info string: the image format
	 * as a bare word ("svg", "png") or as "format=png", and the diagram's
	 * description as 'title="..."'. Whatever is left out keeps its default.
	 */
	@NonNull
	static Options parseInfo(@NonNull String info) {
		Options options = new Options();
		List<String> wordList;
		try {
			wordList = split(info);
		} catch(IllegalArgumentException x) {
			options.error = INFO_WORD + ": " + x.getMessage();
			return options;
		}

		for(int i = 1; i < wordList.size(); i++) {					// Word 0 is "plantuml" itself
			String word = wordList.get(i);
			int eq = word.indexOf('=');
			String name = eq < 0 ? word : word.substring(0, eq);
			String value = eq < 0 ? null : word.substring(eq + 1);
			String error;
			if(null == value) {
				error = setFormat(options, name, "'" + name + "' is not an option nor an image format");
			} else if("format".equalsIgnoreCase(name)) {
				error = setFormat(options, value, "format=" + value + " is not an image format");
			} else if("title".equalsIgnoreCase(name)) {
				options.title = value;
				error = null;
			} else {
				error = "unknown option '" + name + "'";
			}
			if(null != error) {
				options.error = INFO_WORD + ": " + error + "; the block takes an image format (" + PlantumlFormat.names() + ") and title=\"...\"";
				return options;
			}
		}
		return options;
	}

	/**
	 * Set the image format from the way it was written, answering the complaint
	 * to make when that is not a format at all.
	 */
	@Nullable
	private static String setFormat(@NonNull Options options, @NonNull String name, @NonNull String complaint) {
		PlantumlFormat format = PlantumlFormat.byName(name);
		if(null == format)
			return complaint;
		options.format = format;
		return null;
	}

	/**
	 * Split an info string into words, keeping what is inside double quotes
	 * together and dropping the quotes themselves, so that a title can contain
	 * spaces.
	 */
	@NonNull
	static List<String> split(@NonNull String info) {
		List<String> list = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inWord = false;
		boolean inQuote = false;
		for(int i = 0; i < info.length(); i++) {
			char c = info.charAt(i);
			if(c == '"') {
				inQuote = !inQuote;
				inWord = true;
			} else if(!inQuote && Character.isWhitespace(c)) {
				if(inWord) {
					list.add(sb.toString());
					sb.setLength(0);
					inWord = false;
				}
			} else {
				sb.append(c);
				inWord = true;
			}
		}
		if(inQuote)
			throw new IllegalArgumentException("the options have an unclosed \"");
		if(inWord)
			list.add(sb.toString());
		return list;
	}

	/**
	 * What the fence's info string asked for, with the defaults already filled
	 * in, plus the complaint about it when it asked for something impossible.
	 */
	static final class Options {
		@NonNull
		PlantumlFormat format = PlantumlFormat.Svg;

		@Nullable
		String title;

		@Nullable
		String error;
	}
}

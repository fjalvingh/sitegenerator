package to.etc.sigeto.demos;

import org.commonmark.node.CustomBlock;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A "!demo(path)" tag: an iframe showing a page of the live application that
 * belongs with the documentation, at "&lt;include base&gt;/&lt;path&gt;".
 *
 * The include base is not known to the markdown at all - it is a property of
 * the site (the ${demo} variable), because the same documentation is built
 * against different installations of the application it describes.
 */
public class DemoBlock extends CustomBlock {
	/** The size an embedded application page gets when the tag does not say otherwise. */
	public static final String DEFAULT_WIDTH = "1280";

	public static final String DEFAULT_HEIGHT = "800";

	@NonNull
	private final String m_path;

	@NonNull
	private final String m_width;

	@NonNull
	private final String m_height;

	public DemoBlock(@NonNull String path, @NonNull String width, @NonNull String height) {
		m_path = path;
		m_width = width;
		m_height = height;
	}

	@NonNull
	public String getPath() {
		return m_path;
	}

	@NonNull
	public String getWidth() {
		return m_width;
	}

	@NonNull
	public String getHeight() {
		return m_height;
	}

	/**
	 * The url the iframe gets: the path appended to the include base, with
	 * exactly one slash between them. A path that is a full url already is
	 * used as-is, so that a single page can point at another installation.
	 */
	@NonNull
	public static String url(@NonNull String includeBase, @NonNull String path) {
		if(isAbsolute(path))
			return path;
		String base = includeBase;
		while(base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String rest = path;
		while(rest.startsWith("/")) {
			rest = rest.substring(1);
		}
		return rest.isEmpty() ? base : base + "/" + rest;
	}

	public static boolean isAbsolute(@NonNull String path) {
		String lower = path.toLowerCase();
		return lower.startsWith("http://") || lower.startsWith("https://");
	}

	/**
	 * A size as it goes into the iframe's style: a bare number means pixels,
	 * anything else is a css length that is used as it was written. Returns
	 * null if the size is not a length at all, which is what
	 * {@link #checkSize} reports on.
	 */
	@Nullable
	public static String cssLength(@NonNull String size) {
		if(size.isEmpty())
			return null;
		if(size.matches("\\d+"))
			return size + "px";
		if(size.matches("\\d+(\\.\\d+)?(px|%|em|rem|vh|vw|ch)"))
			return size;
		return null;
	}

	/**
	 * The complaint about a size that is not usable, or null when it is fine.
	 */
	@Nullable
	public static String checkSize(@NonNull String size, @NonNull String what) {
		if(null != cssLength(size))
			return null;
		return "!demo(): '" + size + "' is not a usable " + what + "; use a number of pixels (800) or a css length (100%, 40em)";
	}
}

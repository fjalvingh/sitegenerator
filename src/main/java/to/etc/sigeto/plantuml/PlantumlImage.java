package to.etc.sigeto.plantuml;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The result of generating one diagram: the image file's content and the size
 * it says it has, or the reason PlantUML could not make it.
 */
public class PlantumlImage {
	/** Said of an error that cannot be pinned on a line of the diagram. */
	public static final int NO_LINE = -1;

	@Nullable
	private final byte[] m_data;

	private final int m_width;

	private final int m_height;

	@Nullable
	private final String m_error;

	private final int m_errorLine;

	private PlantumlImage(@Nullable byte[] data, int width, int height, @Nullable String error, int errorLine) {
		m_data = data;
		m_width = width;
		m_height = height;
		m_error = error;
		m_errorLine = errorLine;
	}

	@NonNull
	public static PlantumlImage of(@NonNull byte[] data, int width, int height) {
		return new PlantumlImage(data, width, height, null, NO_LINE);
	}

	@NonNull
	public static PlantumlImage error(@NonNull String error, int errorLine) {
		return new PlantumlImage(null, 0, 0, error, errorLine);
	}

	/** The image file's content, null when the diagram could not be generated. */
	@Nullable
	public byte[] getData() {
		return m_data;
	}

	/** The image's own width in pixels, 0 when it is not known. */
	public int getWidth() {
		return m_width;
	}

	/** The image's own height in pixels, 0 when it is not known. */
	public int getHeight() {
		return m_height;
	}

	/** Why the diagram could not be generated, or null when it was. */
	@Nullable
	public String getError() {
		return m_error;
	}

	/**
	 * The line the error is on, counted from the start of the source handed to
	 * PlantUML, or {@link #NO_LINE} when the error is not about one line.
	 */
	public int getErrorLine() {
		return m_errorLine;
	}
}

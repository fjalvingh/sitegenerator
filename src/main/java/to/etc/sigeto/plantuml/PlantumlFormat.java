package to.etc.sigeto.plantuml;

import net.sourceforge.plantuml.FileFormat;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The image format a PlantUML diagram is generated in. Svg is the default: a
 * diagram is line art, so it stays sharp at any zoom level and its file is
 * smaller than the bitmap would be.
 */
public enum PlantumlFormat {
	Svg("svg", FileFormat.SVG_DETERMINISTIC),

	Png("png", FileFormat.PNG);

	@NonNull
	private final String m_extension;

	@NonNull
	private final FileFormat m_fileFormat;

	PlantumlFormat(@NonNull String extension, @NonNull FileFormat fileFormat) {
		m_extension = extension;
		m_fileFormat = fileFormat;
	}

	/** The extension the generated image file gets, without the dot. */
	@NonNull
	public String getExtension() {
		return m_extension;
	}

	/**
	 * The PlantUML format to generate. Svg uses the deterministic variant, so
	 * that building the same site twice produces byte-identical images.
	 */
	@NonNull
	public FileFormat getFileFormat() {
		return m_fileFormat;
	}

	/**
	 * The format written as "svg" or "png" in the document, or null when the
	 * name is not a format at all.
	 */
	@Nullable
	public static PlantumlFormat byName(@NonNull String name) {
		for(PlantumlFormat format : values()) {
			if(format.m_extension.equalsIgnoreCase(name))
				return format;
		}
		return null;
	}

	/** The formats as they can be written, for use in an error message. */
	@NonNull
	public static String names() {
		StringBuilder sb = new StringBuilder();
		for(PlantumlFormat format : values()) {
			if(sb.length() > 0)
				sb.append(", ");
			sb.append(format.m_extension);
		}
		return sb.toString();
	}
}

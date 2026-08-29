package to.etc.sigeto.plantuml;

import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.ErrorUml;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.core.ImageData;
import net.sourceforge.plantuml.error.PSystemError;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the images for {@link PlantumlBlock}s, and remembers them. Every
 * diagram is looked at twice - once when the documentation is checked and once
 * when it is rendered - and a diagram nested in a blog story is rendered a
 * third time into the sitewide timeline; PlantUML is by far the slowest part of
 * the build, so it runs once per distinct diagram.
 */
public class PlantumlRenderCache {
	static {
		//-- PlantUML measures its text through awt, which needs no screen but must be told so
		System.setProperty("java.awt.headless", "true");
	}

	/** The width and height an svg says it has, in its root element; they can have a fraction. */
	private static final Pattern SVG_SIZE = Pattern.compile("<svg\\b[^>]*?\\bwidth=\"(\\d+(?:\\.\\d+)?)(?:px)?\"[^>]*?\\bheight=\"(\\d+(?:\\.\\d+)?)(?:px)?\"", Pattern.CASE_INSENSITIVE);

	private final Map<String, PlantumlImage> m_imageMap = new HashMap<>();

	/**
	 * The image for this diagram, generating it the first time it is asked for.
	 */
	@NonNull
	public PlantumlImage getImage(@NonNull PlantumlBlock block) {
		String key = block.getFormat().name() + "\n" + block.getSource();
		return m_imageMap.computeIfAbsent(key, a -> render(block));
	}

	@NonNull
	private static PlantumlImage render(@NonNull PlantumlBlock block) {
		try {
			SourceStringReader reader = new SourceStringReader(block.getSource());
			List<BlockUml> blockList = reader.getBlocks();
			if(blockList.isEmpty())
				return PlantumlImage.error("the block does not contain a diagram", PlantumlImage.NO_LINE);

			Diagram diagram = blockList.get(0).getDiagram();
			if(diagram instanceof PSystemError)
				return syntaxError((PSystemError) diagram);

			ByteArrayOutputStream bos = new ByteArrayOutputStream(65536);
			ImageData data = diagram.exportDiagram(bos, 0, new FileFormatOption(block.getFormat().getFileFormat()));
			byte[] bytes = bos.toByteArray();
			return PlantumlImage.of(bytes, width(block, bytes, data), height(block, bytes, data));
		} catch(Exception x) {
			return PlantumlImage.error("the diagram cannot be generated: " + x, PlantumlImage.NO_LINE);
		}
	}

	/**
	 * PlantUML does not throw on a diagram it cannot parse: it hands back a
	 * diagram that draws the error message. Report what it says instead, so the
	 * build stops on it the way it does on a dangling link.
	 */
	@NonNull
	private static PlantumlImage syntaxError(@NonNull PSystemError error) {
		ErrorUml first = error.getFirstError();
		if(null == first)
			return PlantumlImage.error("the diagram has a syntax error", PlantumlImage.NO_LINE);
		return PlantumlImage.error(first.getError(), first.getPosition());
	}

	/**
	 * The image's width. An svg says its own size a pixel larger than the
	 * diagram it drew, and that is the size the browser lays it out at, so for
	 * svg the file itself is asked rather than PlantUML.
	 */
	private static int width(@NonNull PlantumlBlock block, @NonNull byte[] data, @NonNull ImageData imageData) {
		Integer size = svgSize(block, data, 1);
		return null == size ? imageData.getWidth() : size.intValue();
	}

	private static int height(@NonNull PlantumlBlock block, @NonNull byte[] data, @NonNull ImageData imageData) {
		Integer size = svgSize(block, data, 2);
		return null == size ? imageData.getHeight() : size.intValue();
	}

	/**
	 * The width (group 1) or height (group 2) the svg states in its root
	 * element, or null when the image is not an svg or does not state it.
	 */
	@Nullable
	private static Integer svgSize(@NonNull PlantumlBlock block, @NonNull byte[] data, int group) {
		if(block.getFormat() != PlantumlFormat.Svg)
			return null;
		String head = new String(data, 0, Math.min(data.length, 2048), java.nio.charset.StandardCharsets.UTF_8);
		Matcher matcher = SVG_SIZE.matcher(head);
		if(!matcher.find())
			return null;
		try {
			return Integer.valueOf((int) Math.ceil(Double.parseDouble(matcher.group(group))));
		} catch(NumberFormatException x) {
			return null;
		}
	}
}

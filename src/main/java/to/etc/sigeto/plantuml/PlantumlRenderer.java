package to.etc.sigeto.plantuml;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.eclipse.jdt.annotation.NonNull;
import to.etc.sigeto.Util;
import to.etc.sigeto.unidiot.WrappedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Renders a {@link PlantumlBlock} by writing the generated diagram as an image
 * file next to the page that uses it, and embedding that image.
 */
public class PlantumlRenderer implements NodeRenderer {
	private final HtmlWriter m_writer;

	@NonNull
	private final PlantumlRenderCache m_cache;

	/** The root of the generated site, which is where the image files go. */
	@NonNull
	private final File m_outputRoot;

	/** Output-relative directory of the page being rendered. */
	@NonNull
	private final String m_outputDir;

	/** The page's file name without its extension, which the images are named after. */
	@NonNull
	private final String m_baseName;

	/** The number of diagrams rendered on this page so far, which numbers their files. */
	private int m_count;

	public PlantumlRenderer(HtmlNodeRendererContext context, @NonNull PlantumlRenderCache cache, @NonNull File outputRoot, @NonNull String outputDir, @NonNull String baseName) {
		m_writer = context.getWriter();
		m_cache = cache;
		m_outputRoot = outputRoot;
		m_outputDir = outputDir;
		m_baseName = baseName;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Collections.singleton(PlantumlBlock.class);
	}

	@Override
	public void render(Node node) {
		PlantumlBlock block = (PlantumlBlock) node;
		if(null != block.getOptionError())
			return;													// Already reported as an error by the check phase
		PlantumlImage image = m_cache.getImage(block);
		byte[] data = image.getData();
		if(null == data)
			return;													// Ditto

		try {
			String name = writeImage(block, data);
			m_writer.line();
			m_writer.tag("div", Util.attributes("class", "ui-uml"));
			m_writer.tag("img", imageAttributes(name, block, image));
			m_writer.tag("/div");
			m_writer.line();
		} catch(Exception x) {
			throw WrappedException.wrap(x);
		}
	}

	/**
	 * Write the generated image into the page's own output directory, and
	 * answer the name it got there - which is also the url the page uses for
	 * it, the two being in the same directory.
	 */
	@NonNull
	private String writeImage(@NonNull PlantumlBlock block, @NonNull byte[] data) throws Exception {
		m_count++;
		String name = m_baseName + "-uml" + m_count + "." + block.getFormat().getExtension();
		File dir = m_outputDir.isEmpty() ? m_outputRoot : new File(m_outputRoot, m_outputDir);
		dir.mkdirs();
		try(OutputStream os = new FileOutputStream(new File(dir, name))) {
			os.write(data);
		}
		return name;
	}

	/**
	 * The image's attributes, including the size it is shown at: the size the
	 * diagram actually has, scaled down when it is wider than a page can show.
	 * Stating the size keeps the page from jumping around while the images
	 * still load.
	 */
	@NonNull
	private Map<String, String> imageAttributes(@NonNull String name, @NonNull PlantumlBlock block, @NonNull PlantumlImage image) {
		Map<String, String> map = Util.attributes("src", name, "alt", block.getAltText());
		String title = block.getTitle();
		if(null != title) {
			map.put("title", title);
		}
		int width = image.getWidth();
		int height = image.getHeight();
		if(width <= 0 || height <= 0)
			return map;
		if(width > PlantumlBlock.MAX_WIDTH) {
			double factor = (double) PlantumlBlock.MAX_WIDTH / width;
			width = PlantumlBlock.MAX_WIDTH;
			height = (int) (height * factor);
		}
		map.put("width", Integer.toString(width));
		map.put("height", Integer.toString(height));
		return map;
	}
}

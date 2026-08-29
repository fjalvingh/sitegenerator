package to.etc.sigeto.plantuml;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;

/**
 * The "```plantuml" fenced block, which generates a diagram from its content
 * and embeds it as an image. See {@link PlantumlBlock} for the syntax.
 *
 * Where the image file is written is only known while a page is rendered, so
 * the parsing half and the rendering half are created separately: {@link
 * #create(PlantumlRenderCache)} for the parser, and {@link
 * #create(PlantumlRenderCache, File, String, String)} per page for the
 * renderer.
 */
public class PlantumlExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	@NonNull
	private final PlantumlRenderCache m_cache;

	/** Null in the parser's copy, which does not render anything. */
	@Nullable
	private final File m_outputRoot;

	@Nullable
	private final String m_outputDir;

	@Nullable
	private final String m_baseName;

	private PlantumlExtension(@NonNull PlantumlRenderCache cache, @Nullable File outputRoot, @Nullable String outputDir, @Nullable String baseName) {
		m_cache = cache;
		m_outputRoot = outputRoot;
		m_outputDir = outputDir;
		m_baseName = baseName;
	}

	public static Extension create(@NonNull PlantumlRenderCache cache) {
		return new PlantumlExtension(cache, null, null, null);
	}

	/**
	 * The extension for rendering a page: baseName is the page's file name
	 * without its extension, which the images it uses are named after, and
	 * outputDir the directory below outputRoot the page is written to.
	 */
	public static Extension create(@NonNull PlantumlRenderCache cache, @NonNull File outputRoot, @NonNull String outputDir, @NonNull String baseName) {
		return new PlantumlExtension(cache, outputRoot, outputDir, baseName);
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.postProcessor(new PlantumlPostProcessor());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		File outputRoot = m_outputRoot;
		String outputDir = m_outputDir;
		String baseName = m_baseName;
		if(null == outputRoot || null == outputDir || null == baseName)
			return;													// The parser's copy: nothing to render with
		rendererBuilder.nodeRendererFactory(context -> new PlantumlRenderer(context, m_cache, outputRoot, outputDir, baseName));
	}
}

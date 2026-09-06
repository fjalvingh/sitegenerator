package to.etc.sigeto.figures;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Renders an image that stands alone in its paragraph as a captioned
 * &lt;figure&gt;. See {@link FigureBlock} for what counts as standing alone.
 */
final public class FigureExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	private FigureExtension() {
	}

	public static Extension create() {
		return new FigureExtension();
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.postProcessor(new FigurePostProcessor());
	}

	@Override
	public void extend(HtmlRenderer.Builder htmlBuilder) {
		htmlBuilder.nodeRendererFactory(context -> new FigureRenderer(context));
	}
}

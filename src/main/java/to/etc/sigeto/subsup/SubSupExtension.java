package to.etc.sigeto.subsup;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;

/**
 * Adds "~subscript~" and "^superscript^", rendered as &lt;sub&gt; and
 * &lt;sup&gt;.
 *
 * <p>The subscript half shares its delimiter with strikethrough, which is why
 * the strikethrough extension is registered with requireTwoTildes: with both
 * claiming a run of one "~" the parser refuses to accept them together.</p>
 */
final public class SubSupExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension, TextContentRenderer.TextContentRendererExtension {
	private SubSupExtension() {
	}

	public static Extension create() {
		return new SubSupExtension();
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customDelimiterProcessor(new SubSupDelimiterProcessor('~', "sub"));
		parserBuilder.customDelimiterProcessor(new SubSupDelimiterProcessor('^', "sup"));
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(SubSupRenderer::new);
	}

	@Override
	public void extend(TextContentRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(SubSupTextRenderer::new);
	}
}

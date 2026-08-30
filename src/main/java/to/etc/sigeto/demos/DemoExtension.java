package to.etc.sigeto.demos;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The "!demo(path)" tag, which embeds a page of a live application in the
 * documentation for it. See {@link DemoParser} for the syntax and
 * {@link DemoBlock} for how the url is made.
 */
public class DemoExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	/** The base url the paths in the tags are resolved against, from ${demo}; null when the site defined none. */
	@Nullable
	private final String m_includeBase;

	private DemoExtension(@Nullable String includeBase) {
		m_includeBase = includeBase;
	}

	public static Extension create(@Nullable String includeBase) {
		return new DemoExtension(includeBase);
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customBlockParserFactory(new DemoParser.Factory());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(context -> new DemoRenderer(context, m_includeBase));
	}
}

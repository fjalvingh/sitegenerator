package to.etc.sigeto.variables;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.jdt.annotation.NonNull;

import java.util.function.Function;

/**
 * "${name}" variables, replaced verbatim by whatever the resolver says the
 * name stands for. This extension handles the variables used in the text of a
 * document; the ones used in a link or image url are not part of the parse and
 * are handled by {@link VariableExpander}.
 */
public class VariableExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	/** What a name stands for; returns null for a name that is not defined. */
	@NonNull
	private final Function<String, String> m_resolver;

	private VariableExtension(@NonNull Function<String, String> resolver) {
		m_resolver = resolver;
	}

	public static Extension create(@NonNull Function<String, String> resolver) {
		return new VariableExtension(resolver);
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customInlineContentParserFactory(new VariableParser.Factory());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(context -> new VariableRenderer(context, m_resolver));
	}
}

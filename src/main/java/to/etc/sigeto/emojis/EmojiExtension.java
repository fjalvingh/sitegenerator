package to.etc.sigeto.emojis;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

final public class EmojiExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension /*, TextContentRenderer.TextContentRendererExtension, MarkdownRenderer.MarkdownRendererExtension */ {
	private EmojiExtension() {
	}

	public static Extension create() {
		return new EmojiExtension();
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customDelimiterProcessor(new EmojiDelimiterProcessor());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(EmojiRenderer::new);
	}
}


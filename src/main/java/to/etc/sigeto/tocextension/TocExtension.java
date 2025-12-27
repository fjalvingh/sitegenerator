package to.etc.sigeto.tocextension;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class TocExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension /*, TextContentRenderer.TextContentRendererExtension, MarkdownRenderer.MarkdownRendererExtension */ {
	private TocExtension() {
	}

	public static Extension create() {
		return new TocExtension();
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customBlockParserFactory(new TocParser.Factory());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(TocHtmlRenderer::new);
	}

	//@Override
	//public void extend(TextContentRenderer.Builder rendererBuilder) {
	//	rendererBuilder.nodeRendererFactory(new TextContentNodeRendererFactory() {
	//		@Override
	//		public NodeRenderer create(TextContentNodeRendererContext context) {
	//			return new TableTextContentNodeRenderer(context);
	//		}
	//	});
	//}

	//@Override
	//public void extend(MarkdownRenderer.Builder rendererBuilder) {
	//	rendererBuilder.nodeRendererFactory(new MarkdownNodeRendererFactory() {
	//		@Override
	//		public NodeRenderer create(MarkdownNodeRendererContext context) {
	//			return new TableMarkdownNodeRenderer(context);
	//		}
	//
	//		@Override
	//		public Set<Character> getSpecialCharacters() {
	//			return Set.of('|');
	//		}
	//	});
	//}
}

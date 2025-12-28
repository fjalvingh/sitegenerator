package to.etc.sigeto.blogextension;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.sigeto.ContentItem;

public class BlogExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension /*, TextContentRenderer.TextContentRendererExtension, MarkdownRenderer.MarkdownRendererExtension */ {
	@Nullable
	private final ContentItem m_item;

	private BlogExtension(@Nullable ContentItem item) {
		m_item = item;
	}

	public static Extension create() {
		return new BlogExtension(null);
	}

	public static Extension create(ContentItem item) {
		return new BlogExtension(item);
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customBlockParserFactory(new BlogParser.Factory());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(context -> new BlogHtmlRenderer(context, m_item));
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

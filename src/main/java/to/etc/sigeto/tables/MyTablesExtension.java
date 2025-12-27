package to.etc.sigeto.tables;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.internal.TableBlockParser;
import org.commonmark.ext.gfm.tables.internal.TableMarkdownNodeRenderer;
import org.commonmark.ext.gfm.tables.internal.TableTextContentNodeRenderer;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext;
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory;
import org.commonmark.renderer.markdown.MarkdownRenderer;
import org.commonmark.renderer.text.TextContentNodeRendererContext;
import org.commonmark.renderer.text.TextContentNodeRendererFactory;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.Set;

/**
 * Override the tables extension with a new renderer that adds css classes.
 */
public class MyTablesExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension, TextContentRenderer.TextContentRendererExtension, MarkdownRenderer.MarkdownRendererExtension {
	private MyTablesExtension() {
	}

	public static Extension create() {
		return new MyTablesExtension();
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customBlockParserFactory(new TableBlockParser.Factory());
	}

	@Override
	public void extend(HtmlRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(MyTableHtmlNodeRenderer::new);
	}

	@Override
	public void extend(TextContentRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(new TextContentNodeRendererFactory() {
			@Override
			public NodeRenderer create(TextContentNodeRendererContext context) {
				return new TableTextContentNodeRenderer(context);
			}
		});
	}

	@Override
	public void extend(MarkdownRenderer.Builder rendererBuilder) {
		rendererBuilder.nodeRendererFactory(new MarkdownNodeRendererFactory() {
			@Override
			public NodeRenderer create(MarkdownNodeRendererContext context) {
				return new TableMarkdownNodeRenderer(context);
			}

			@Override
			public Set<Character> getSpecialCharacters() {
				return Set.of('|');
			}
		});
	}
}

package to.etc.sigeto.emojis;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Map;
import java.util.Set;

public class EmojiRenderer implements NodeRenderer {
	private final HtmlWriter m_htmlWriter;

	public EmojiRenderer(HtmlNodeRendererContext context) {
		m_htmlWriter = context.getWriter();
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(EmojiNode.class);
	}

	@Override
	public void render(Node node) {
		EmojiNode emoji = (EmojiNode) node;

		//-- We need an inline img.
		m_htmlWriter.tag("img", Map.of(
			"src", emoji.getRef().getUrl(),
			"width", "16",
			"height", "16",
			"align", "center"
		));
	}
}

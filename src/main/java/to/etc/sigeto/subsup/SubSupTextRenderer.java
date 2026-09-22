package to.etc.sigeto.subsup;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.text.TextContentNodeRendererContext;

import java.util.Set;

/**
 * Renders the content of a sub/superscript as plain text, so that a heading
 * holding one still yields its full text as the page title.
 */
public class SubSupTextRenderer implements NodeRenderer {
	private final TextContentNodeRendererContext m_context;

	public SubSupTextRenderer(TextContentNodeRendererContext context) {
		m_context = context;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(SubSupNode.class);
	}

	@Override
	public void render(Node node) {
		Node child = node.getFirstChild();
		while(child != null) {
			Node next = child.getNext();
			m_context.render(child);
			child = next;
		}
	}
}

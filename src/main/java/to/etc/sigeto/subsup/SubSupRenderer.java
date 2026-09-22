package to.etc.sigeto.subsup;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Map;
import java.util.Set;

public class SubSupRenderer implements NodeRenderer {
	private final HtmlNodeRendererContext m_context;

	private final HtmlWriter m_htmlWriter;

	public SubSupRenderer(HtmlNodeRendererContext context) {
		m_context = context;
		m_htmlWriter = context.getWriter();
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(SubSupNode.class);
	}

	@Override
	public void render(Node node) {
		SubSupNode subSup = (SubSupNode) node;
		String tagName = subSup.getTagName();
		Map<String, String> attributes = m_context.extendAttributes(node, tagName, Map.of());
		m_htmlWriter.tag(tagName, attributes);
		renderChildren(node);
		m_htmlWriter.tag("/" + tagName);
	}

	private void renderChildren(Node parent) {
		Node node = parent.getFirstChild();
		while(node != null) {
			Node next = node.getNext();
			m_context.render(node);
			node = next;
		}
	}
}

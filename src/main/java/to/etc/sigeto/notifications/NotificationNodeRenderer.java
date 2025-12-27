package to.etc.sigeto.notifications;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NotificationNodeRenderer implements NodeRenderer {
	private final HtmlNodeRendererContext m_context;

	private final HtmlWriter m_htmlWriter;

	public NotificationNodeRenderer(HtmlNodeRendererContext context) {
		m_context = context;
		m_htmlWriter = context.getWriter();
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Collections.singleton(NotificationBlock.class);
	}

	@Override
	public void render(Node node) {
		NotificationBlock nb = (NotificationBlock) node;

		m_htmlWriter.line();
		Notification n = nb.getType();
		m_htmlWriter.tag("div", Map.of("class", "ui-not ui-not-" + nb.getType().name().toLowerCase()));

		m_htmlWriter.tag("div", Map.of("class", "ui-not-icon"));
		//m_htmlWriter.tag("img", Map.of("src", "/img/notif-icon-" + nb.getType().name().toLowerCase() + ".png"));
		m_htmlWriter.tag("/div");

		m_htmlWriter.tag("div", Map.of("class", "ui-not-cnt"));
		renderChildren(nb);
		m_htmlWriter.tag("/div");
		m_htmlWriter.tag("/div");
		m_htmlWriter.line();
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

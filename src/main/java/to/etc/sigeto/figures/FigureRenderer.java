package to.etc.sigeto.figures;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import to.etc.sigeto.Util;

import java.util.Collections;
import java.util.Set;

public class FigureRenderer implements NodeRenderer {
	private final HtmlNodeRendererContext m_context;

	private final HtmlWriter m_htmlWriter;

	public FigureRenderer(HtmlNodeRendererContext context) {
		m_context = context;
		m_htmlWriter = context.getWriter();
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Collections.singleton(FigureBlock.class);
	}

	@Override
	public void render(Node node) {
		FigureBlock fb = (FigureBlock) node;
		m_htmlWriter.line();
		m_htmlWriter.tag("figure", Util.attributes("class", "ui-figure"));
		renderChildren(fb);											// The image, which MdImgRenderer sizes and links
		String caption = fb.getCaption();
		if(!caption.isBlank()) {
			m_htmlWriter.tag("figcaption");
			m_htmlWriter.text(caption);
			m_htmlWriter.tag("/figcaption");
		}
		m_htmlWriter.tag("/figure");
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

package to.etc.sigeto.demos;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Renders a {@link DemoBlock} as the iframe showing that page of the live
 * application. The div around it is what a stylesheet gets to work with: an
 * iframe with a fixed pixel size cannot be made to fit a narrow screen by css
 * alone.
 */
public class DemoRenderer implements NodeRenderer {
	private final HtmlWriter m_writer;

	/** The -include base url, or null when the build did not get one. */
	@Nullable
	private final String m_includeBase;

	public DemoRenderer(HtmlNodeRendererContext context, @Nullable String includeBase) {
		m_writer = context.getWriter();
		m_includeBase = includeBase;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Collections.singleton(DemoBlock.class);
	}

	@Override
	public void render(Node node) {
		DemoBlock demo = (DemoBlock) node;
		String base = m_includeBase;
		if(null == base) {
			return;													// Already reported as an error by the check phase
		}
		String width = DemoBlock.cssLength(demo.getWidth());
		String height = DemoBlock.cssLength(demo.getHeight());
		if(null == width || null == height) {
			return;													// Ditto
		}

		Map<String, String> attributes = new LinkedHashMap<>();
		attributes.put("class", "ui-demo-frame");
		attributes.put("src", DemoBlock.url(base, demo.getPath()));
		attributes.put("style", "width: " + width + "; height: " + height + ";");
		attributes.put("loading", "lazy");
		attributes.put("title", demo.getPath());

		m_writer.line();
		m_writer.tag("div", Map.of("class", "ui-demo"));
		m_writer.tag("iframe", attributes);
		m_writer.tag("/iframe");
		m_writer.tag("/div");
		m_writer.line();
	}
}

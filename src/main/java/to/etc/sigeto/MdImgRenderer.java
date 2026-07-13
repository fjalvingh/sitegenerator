package to.etc.sigeto;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.eclipse.jdt.annotation.NonNull;
import to.etc.sigeto.unidiot.WrappedException;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final public class MdImgRenderer implements NodeRenderer {
	private final ContentItem m_item;

	private final String m_outputDir;

	private final HtmlWriter m_writer;

	private Dimension m_maxImageSize = new Dimension(900, 900);

	MdImgRenderer(ContentItem item, String outputDir, HtmlNodeRendererContext context) {
		m_item = item;
		m_outputDir = outputDir;
		m_writer = context.getWriter();
	}

	@Override public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(Image.class);
	}

	@Override
	public void render(Node node) {
		Image img = (Image) node;
		fixImage(img);
	}

	private void fixImage(@NonNull Image node) {
		try {
			String url = node.getDestination();
			String alt = altText(node);
			if(Content.isRelativePath(url)) {
				ContentItem item = m_item.findItemByURL(url);
				if(null != item) {
					//BufferedImage srcBi = ImageIO.read(item.getFile());            // Load the image
					String href = Util.relativeHref(m_outputDir, item.getRelativeTargetPath());

					Dimension sz = Util.getImageDimension(item.getFile());
					Dimension maxImageSize = m_maxImageSize;
					if(sz.getWidth() > maxImageSize.width) {                    // Only limit width
						double factor = (double) maxImageSize.width / sz.getWidth();
						int nw = (int) (sz.getWidth() * factor);
						int nh = (int) (sz.getHeight() * factor);                // New size

						m_writer.tag("a", Map.of("href", href, "class", "ui-im-l"));
						m_writer.tag("img", imgAttributes(href, alt,
							"width", Integer.toString(nw),
							"height", Integer.toString(nh)
						));
						m_writer.tag("/img");
						m_writer.tag("/a");
					} else {
						//-- Write the original, but add the size for better rendering
						m_writer.tag("img", imgAttributes(href, alt,
							"width", Integer.toString(sz.width),
							"height", Integer.toString(sz.height)
						));
						m_writer.tag("/img");
					}
				}
			} else {
				//-- External image: dimensions cannot be probed, so render it as-is.
				m_writer.tag("img", imgAttributes(url, alt));
				m_writer.tag("/img");
			}

		} catch(Exception e) {
			throw WrappedException.wrap(e);
		}
	}

	/**
	 * Concatenates the Text/Code children of the Image node, which is how commonmark
	 * represents an image's alt text (the "..." in ![...](url)).
	 */
	private String altText(Image node) {
		StringBuilder sb = new StringBuilder();
		node.accept(new AbstractVisitor() {
			@Override public void visit(Text text) {
				sb.append(text.getLiteral());
			}

			@Override public void visit(Code code) {
				sb.append(code.getLiteral());
			}
		});
		return sb.toString();
	}

	private Map<String, String> imgAttributes(String url, String alt, String... extra) {
		Map<String, String> map = new HashMap<>();
		map.put("src", url);
		map.put("alt", alt);
		for(int i = 0; i < extra.length; i += 2) {
			map.put(extra[i], extra[i + 1]);
		}
		return map;
	}
}

package to.etc.sigeto;

import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.eclipse.jdt.annotation.NonNull;
import to.etc.sigeto.unidiot.WrappedException;

import java.awt.*;
import java.util.Map;
import java.util.Set;

final public class MdImgRenderer implements NodeRenderer {
	private final Content m_content;

	private final ContentItem m_item;

	private final HtmlWriter m_writer;

	private Dimension m_maxImageSize = new Dimension(900, 900);

	MdImgRenderer(Content content, ContentItem item, HtmlNodeRendererContext context) {
		m_content = content;
		m_item = item;
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
			if(Content.isRelativePath(url)) {
				ContentItem item = m_item.findItemByURL(url);
				if(null != item) {
					//BufferedImage srcBi = ImageIO.read(item.getFile());            // Load the image

					Dimension sz = Util.getImageDimension(item.getFile());
					Dimension maxImageSize = m_maxImageSize;
					if(sz.getWidth() > maxImageSize.width) {                    // Only limit width
						double factor = (double) maxImageSize.width / sz.getWidth();
						int nw = (int) (sz.getWidth() * factor);
						int nh = (int) (sz.getHeight() * factor);                // New size

						m_writer.tag("a", Map.of("href", url, "class", "ui-im-l"));
						m_writer.tag("img", Map.of(
							"src", url,
							"width", Integer.toString(nw),
							"height", Integer.toString(nh)
						));
						m_writer.tag("/img");
						m_writer.tag("/a");
					} else {
						//-- Write the original, but add the size for better rendering
						m_writer.tag("img", Map.of(
							"src", url,
							"width", Integer.toString(sz.width),
							"height", Integer.toString(sz.height)
						));
						m_writer.tag("/img");
					}
				}
			}

		} catch(Exception e) {
			throw WrappedException.wrap(e);
		}
	}
}

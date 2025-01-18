package to.etc.sigeto;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.DelegatingNodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import to.etc.sigeto.unidiot.WrappedException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

final public class MdImageRewriter implements NodeRenderer {
	private final MarkdownChecker m_content;

	public MdImageRewriter(MarkdownChecker content) {
		m_content = content;
	}

	public static class Factory implements DelegatingNodeRendererFactory {
		private final MarkdownChecker m_content;

		public Factory(MarkdownChecker content) {
			m_content = content;
		}

		@NotNull
		@Override
		public NodeRenderer apply(@NotNull DataHolder options) {
			return new MdImageRewriter(m_content);
		}

		@Override
		public Set<Class<?>> getDelegates() {
			///Set<Class<?>>();
			// add node renderer factory classes to which this renderer will delegate some of its rendering
			// core node renderer is assumed to have all depend it so there is no need to add it
			//set.add(WikiLinkNodeRenderer.Factory.class);
			//return set;

			// return null if renderer does not delegate or delegates only to core node renderer
			return null;
		}
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		HashSet<NodeRenderingHandler<?>> set = new HashSet<>();
		set.add(new NodeRenderingHandler<>(Image.class, (node, context, html) -> {
			try {
				String url = node.getUrl().unescape();
				if(Content.isRelativePath(url)) {
					ContentItem item = m_content.findItemByURL(url);
					if(null != item) {
						BufferedImage srcBi = ImageIO.read(item.getFile());			// Load the image
						Dimension maxImageSize = m_content.getMaxImageSize();
						if(srcBi.getWidth() > maxImageSize.width) {					// Only limit width
							double factor = (double)maxImageSize.width / srcBi.getWidth();
							int nw = (int)(srcBi.getWidth() * factor);
							int nh = (int)(srcBi.getHeight() * factor);				// New size

							html
								.withAttr()
								.attr("href", url)
								.attr("class", "ui-im-l")
								.tag("a");

							html
								.withAttr()
								.attr("src", url)
								.attr("width", Integer.toString(nw))
								.attr("height", Integer.toString(nh))
								.tag("img");
							html.closeTag("img");
							html.closeTag("a");
							return;
						} else {
							//-- Write the original, but add the size for better rendering
							html
								.withAttr()
								.attr("src", url)
								.attr("width", Integer.toString(srcBi.getWidth()))
								.attr("height", Integer.toString(srcBi.getHeight()))
								.tag("img");
							html.closeTag("img");
							return;
						}
					}
				}

				//-- Delegate to default
				context.delegateRender();


				// test the node to see if it needs overriding
				html.tag("img");
				html.closeTag("img");
				//if(node.getText().equals("bar")) {
				//	html.text("(eliminated)");
				//} else {
				//	// otherwise pass it for default rendering
				//	context.delegateRender();
				//}
			} catch(Exception e) {
				throw WrappedException.wrap(e);
			}
		}));
		//set.add(new NodeRenderingHandler<WikiLink>(WikiLink.class, new CustomNodeRenderer<WikiLink>() {
		//    @Override
		//    public void render(WikiLink node, NodeRendererContext context, HtmlWriter html) {
		//        // test the node to see if it needs overriding
		//        Matcher matcher = CONFLUENCE_WIKI_LINK.matcher(node.getChars());
		//        if (matcher.find()) {
		//            String link = "...";
		//            html.raw(link);
		//        } else {
		//            // otherwise pass it for default rendering
		//            context.delegateRender();
		//        }
		//    }
		//}));

		return set;
	}

	static class MdFixImgExtension implements HtmlRenderer.HtmlRendererExtension {
		private final MarkdownChecker m_content;

		public MdFixImgExtension(MarkdownChecker content) {
			m_content = content;
		}

		@Override
		public void rendererOptions(@NotNull MutableDataHolder options) {

		}

		@Override
		public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
			//htmlRendererBuilder.linkResolverFactory(new Factory());
			htmlRendererBuilder.nodeRendererFactory(new Factory(m_content));
		}

		static MdFixImgExtension create(MarkdownChecker content) {
			return new MdFixImgExtension(content);
		}
	}

}

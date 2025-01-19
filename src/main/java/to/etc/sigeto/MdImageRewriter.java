package to.etc.sigeto;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.DelegatingNodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import to.etc.sigeto.unidiot.WrappedException;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

final public class MdImageRewriter implements NodeRenderer {
	private final MarkdownChecker m_content;

	public MdImageRewriter(MarkdownChecker content) {
		m_content = content;
	}

	/**
	 * Make Heading nodes have an ID that is constructed from
	 * the heading text.
	 */
	private void fixHeading(@NotNull Heading node, @NotNull NodeRendererContext context, @NotNull HtmlWriter html) {
		String unescape = node.getText().unescape();
		int level = node.getLevel();
		String id = calculateId(unescape);
		html
			.withAttr()
			.attr("id", id)
			.tag("h" + level);
		html.text(node.getText());
		html.closeTag("h" + level);
	}

	private String calculateId(String text) {
		StringBuilder sb = new StringBuilder();
		text = text.trim().toLowerCase();
		boolean spaceAdded = false;
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(Character.isLetterOrDigit(c)) {
				sb.append(c);
				spaceAdded = false;
			} else if(Character.isWhitespace(c)) {
				if(!spaceAdded) {
					sb.append('-');
					spaceAdded = true;
				}
			}
		}
		return sb.toString();
	}

	private void fixImage(@NotNull Image node, @NotNull NodeRendererContext context, @NotNull HtmlWriter html) {
		try {
			String url = node.getUrl().unescape();
			if(Content.isRelativePath(url)) {
				ContentItem item = m_content.findItemByURL(url);
				if(null != item) {
					//BufferedImage srcBi = ImageIO.read(item.getFile());            // Load the image

					Dimension sz = Util.getImageDimension(item.getFile());
					Dimension maxImageSize = m_content.getMaxImageSize();
					if(sz.getWidth() > maxImageSize.width) {                    // Only limit width
						double factor = (double) maxImageSize.width / sz.getWidth();
						int nw = (int) (sz.getWidth() * factor);
						int nh = (int) (sz.getHeight() * factor);                // New size

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
					} else {
						//-- Write the original, but add the size for better rendering
						html
							.withAttr()
							.attr("src", url)
							.attr("width", Integer.toString(sz.width))
							.attr("height", Integer.toString(sz.height))
							.tag("img");
						html.closeTag("img");
					}
					return;
				}
			}

			//-- Delegate to default
			context.delegateRender();
		} catch(Exception e) {
			throw WrappedException.wrap(e);
		}
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		HashSet<NodeRenderingHandler<?>> set = new HashSet<>();
		set.add(new NodeRenderingHandler<>(Image.class, this::fixImage));
		set.add(new NodeRenderingHandler<>(Heading.class, this::fixHeading));
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
			// return null if renderer does not delegate or delegates only to core node renderer
			return null;
		}
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

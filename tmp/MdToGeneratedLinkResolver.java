package to.etc.sigeto;

import org.commonmark.node.Link;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.nio.file.Path;
import java.util.Set;

/**
 * Thing that, for some stupid reason, is needed to change links.
 */
public class MdToGeneratedLinkResolver implements LinkResolver {
	public MdToGeneratedLinkResolver(LinkResolverBasicContext context) {
	}

	@Override
	public ResolvedLink resolveLink(Node node, LinkResolverBasicContext context, ResolvedLink link) {
		if(node instanceof WikiImage) {
			// resolve wiki image link
			String url = link.getUrl() + ".png";
			return link.withStatus(LinkStatus.VALID).withUrl(url);
		} else if(node instanceof WikiLink) {
			String url = link.getUrl() + ".html";
			return link.withStatus(LinkStatus.VALID).withUrl(url);
		} else if(node instanceof Link) {
			//-- Sigh.
			String url = fixLink(link.getUrl());
			return link.withStatus(LinkStatus.VALID).withUrl(url);
		}
		return link;
	}

	private String fixLink(String url) {
		if(url.startsWith("http:") || url.startsWith("https:")) {
			return url;
		}
		String ext = Util.getExtension(url);
		if(ext.equalsIgnoreCase("md") || ext.equalsIgnoreCase("mdown")) {
			url = Util.getFilenameSansExtension(url) + ".html";
			url = Path.of(url).normalize().toString();
		}
		return url;
	}

	static class Factory implements LinkResolverFactory {
		@Nullable
		@Override
		public Set<Class<?>> getAfterDependents() {
			return null;
		}

		@Nullable
		@Override
		public Set<Class<?>> getBeforeDependents() {
			return null;
		}

		@Override
		public boolean affectsGlobalScope() {
			return false;
		}

		@Override
		public LinkResolver apply(@NotNull LinkResolverBasicContext context) {
			return new MdToGeneratedLinkResolver(context);
		}
	}

	static class MdLinkToGeneratedLinkExtension implements HtmlRenderer.HtmlRendererExtension {
		@Override
		public void rendererOptions(@NotNull MutableDataHolder options) {

		}

		@Override
		public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NonNull String rendererType) {
			htmlRendererBuilder.linkResolverFactory(new MdToGeneratedLinkResolver.Factory());
			//htmlRendererBuilder.nodeRendererFactory(new CustomLinkRenderer.Factory());
		}

		static MdLinkToGeneratedLinkExtension create() {
			return new MdLinkToGeneratedLinkExtension();
		}
	}
}

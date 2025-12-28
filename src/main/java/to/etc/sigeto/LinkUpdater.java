package to.etc.sigeto;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Link;

import java.nio.file.Path;

/**
 * Update link targets for .md files to the generated html file.
 */
final public class LinkUpdater extends AbstractVisitor {
	@Override public void visit(Link link) {
		String url = link.getDestination();
		link.setDestination(fixLink(url));
	}

	static public String fixLink(String url) {
		String lcurl = url.toLowerCase();
		if(lcurl.startsWith("http:") || lcurl.startsWith("https:")) {
			return url;
		}
		String ext = Util.getExtension(lcurl);
		if(ext.equalsIgnoreCase("md") || ext.equalsIgnoreCase("mdown")) {
			url = Util.getFilenameSansExtension(url) + ".html";
			url = Path.of(url).normalize().toString();
		}
		return url;
	}

}

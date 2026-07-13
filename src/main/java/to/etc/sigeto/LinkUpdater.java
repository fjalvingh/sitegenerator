package to.etc.sigeto;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Link;

/**
 * Update link targets for internal links to point at the generated html
 * file, using an href that is correct for the page's actual render output
 * directory (which is not necessarily the same directory the source item
 * was scanned from - see MarkdownChecker.renderContent).
 */
final public class LinkUpdater extends AbstractVisitor {
	private final ContentItem m_currentItem;

	private final String m_outputDir;

	public LinkUpdater(ContentItem currentItem, String outputDir) {
		m_currentItem = currentItem;
		m_outputDir = outputDir;
	}

	@Override public void visit(Link link) {
		link.setDestination(fixLink(link.getDestination()));
	}

	private String fixLink(String url) {
		if(!Content.isRelativePath(url)) {
			return url;
		}
		ContentItem target = m_currentItem.findItemByURL(url);
		if(null == target) {
			return url;
		}
		return Util.relativeHref(m_outputDir, target.getRelativeTargetPath());
	}
}

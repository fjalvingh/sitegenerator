package to.etc.sigeto.variables;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves the "${name}" variables in a parsed document, before anything looks
 * at it.
 *
 * <p>In the text of the document {@link VariableParser} has already recognized
 * them as {@link VariableNode}s, and they are replaced here by the plain text
 * they stand for - so that every renderer sees the value, not just the one
 * rendering the html: the page title and the menu are rendered with a
 * TextContentRenderer, and the heading anchors are made from the text of the
 * heading as well.</p>
 *
 * <p>Link and image urls are not part of the inline parse at all - commonmark
 * hands a url over as the literal text it is written as - so the variables in
 * those are expanded here too, which means the link checker, the link updater
 * and the image renderer all see resolved urls only.</p>
 */
final public class VariableExpander extends AbstractVisitor {
	@NonNull
	private final Function<String, String> m_resolver;

	/** For every node that used a variable in its url: the url as it is written in the source. */
	@NonNull
	private final Map<Node, String> m_sourceUrlMap;

	/** For every node whose url used variables that are not defined: those names. */
	@NonNull
	private final Map<Node, List<String>> m_unknownMap;

	public VariableExpander(@NonNull Function<String, String> resolver, @NonNull Map<Node, String> sourceUrlMap, @NonNull Map<Node, List<String>> unknownMap) {
		m_resolver = resolver;
		m_sourceUrlMap = sourceUrlMap;
		m_unknownMap = unknownMap;
	}

	@Override public void visit(CustomNode node) {
		if(node instanceof VariableNode) {
			replaceVariable((VariableNode) node);
			return;
		}
		visitChildren(node);
	}

	/**
	 * Replace a variable in the text by the plain text it stands for. One that
	 * is not defined is left as it is, for the check phase to report.
	 */
	private void replaceVariable(@NonNull VariableNode node) {
		String value = m_resolver.apply(node.getName());
		if(null == value)
			return;
		node.insertBefore(new Text(value));
		node.unlink();
	}

	@Override public void visit(Link link) {
		link.setDestination(expandUrl(link, link.getDestination()));
		link.setTitle(expandTitle(link, link.getTitle()));
		visitChildren(link);
	}

	@Override public void visit(Image image) {
		image.setDestination(expandUrl(image, image.getDestination()));
		image.setTitle(expandTitle(image, image.getTitle()));
		visitChildren(image);
	}

	/**
	 * Expand the url, remembering the original when it actually changed:
	 * errors have to name the url the way the author wrote it, and the source
	 * link fixer has to be able to find it back in the file.
	 */
	@NonNull
	private String expandUrl(@NonNull Node node, @NonNull String url) {
		String expanded = expand(node, url);
		if(!expanded.equals(url)) {
			m_sourceUrlMap.put(node, url);
		}
		return expanded;
	}

	@Nullable
	private String expandTitle(@NonNull Node node, @Nullable String title) {
		return null == title ? null : expand(node, title);
	}

	@NonNull
	private String expand(@NonNull Node node, @NonNull String text) {
		return Variables.expand(text, m_resolver, name -> m_unknownMap.computeIfAbsent(node, a -> new ArrayList<>()).add(name));
	}
}

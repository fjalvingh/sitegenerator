package to.etc.sigeto.variables;

import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.eclipse.jdt.annotation.NonNull;

import java.util.Set;
import java.util.function.Function;

/**
 * Writes what a {@link VariableNode} stands for. The value is written as text,
 * so a value containing html or markdown appears as itself rather than being
 * interpreted.
 */
final public class VariableRenderer implements NodeRenderer {
	@NonNull
	private final HtmlWriter m_writer;

	@NonNull
	private final Function<String, String> m_resolver;

	public VariableRenderer(@NonNull HtmlNodeRendererContext context, @NonNull Function<String, String> resolver) {
		m_writer = context.getWriter();
		m_resolver = resolver;
	}

	@Override public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(VariableNode.class);
	}

	@Override
	public void render(Node node) {
		VariableNode variable = (VariableNode) node;
		String value = m_resolver.apply(variable.getName());
		if(null == value) {
			m_writer.text(variable.toString());						// Already reported as an error by the check phase
			return;
		}
		m_writer.text(value);
	}
}

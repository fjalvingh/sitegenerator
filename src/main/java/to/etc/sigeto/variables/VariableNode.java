package to.etc.sigeto.variables;

import org.commonmark.node.CustomNode;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A "${name}" variable used in the text of a document. What it stands for is
 * decided when the document is rendered, see {@link VariableRenderer}.
 */
final public class VariableNode extends CustomNode {
	@NonNull
	private final String m_name;

	public VariableNode(@NonNull String name) {
		m_name = name;
	}

	@NonNull
	public String getName() {
		return m_name;
	}

	@Override public String toString() {
		return "${" + m_name + "}";
	}
}

package to.etc.sigeto.blogextension;

import org.commonmark.node.CustomBlock;

import java.util.Set;

public class BlogNode extends CustomBlock {
	private final Set<String> m_options;

	public BlogNode(Set<String> options) {
		m_options = options;
	}

	public Set<String> getOptions() {
		return m_options;
	}
}

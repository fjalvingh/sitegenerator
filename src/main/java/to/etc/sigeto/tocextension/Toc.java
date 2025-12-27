package to.etc.sigeto.tocextension;

import org.commonmark.node.CustomBlock;

import java.util.Set;

/**
 * A Table of contents node.
 */
public class Toc extends CustomBlock {
	private final Set<String> m_options;

	public Toc(Set<String> options) {
		m_options = options;
	}

	public Set<String> getOptions() {
		return m_options;
	}
}

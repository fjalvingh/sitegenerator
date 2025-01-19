package to.etc.sigeto;

import java.io.File;

/**
 * Rewrite the site structure from the confluence export
 * format to proper side format.
 */
public class Rewriter {
	private final Content m_content;

	private final MarkdownChecker m_mc;

	private final File m_output;

	public Rewriter(Content content, MarkdownChecker mc, File output) {
		m_content = content;
		m_mc = mc;
		m_output = output;
	}

	public static void rewrite(Content content, MarkdownChecker mc, File output) throws Exception {
		new Rewriter(content, mc, output).run();
	}

	private void run() throws Exception {







	}
}

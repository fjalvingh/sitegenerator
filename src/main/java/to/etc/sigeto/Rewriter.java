package to.etc.sigeto;

import to.etc.sigeto.utils.IndentWriter;

import java.io.File;
import java.io.OutputStreamWriter;

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
		ContentLevel rootLevel = m_content.getPageRootLevel();
		if(null == rootLevel) {
			throw new IllegalStateException("root level is null");
		}

		IndentWriter w = new IndentWriter(new OutputStreamWriter(System.out));
		renderInfo(w, rootLevel);

	}

	private void renderInfo(IndentWriter w, ContentLevel level) throws Exception {
		w.println("Level " + level.getRelativePath());
		w.inc();
		for(ContentItem item : level.getSubItems()) {
			w.println("Item " + item.getRelativePath());
			w.inc();
			for(ContentItem used : item.getUsedItemList()) {
				w.println("Uses " + used.getRelativePath());
			}
			w.dec();
		}
		w.dec();
		w.println("-- sublevels");
		w.inc();
		for(ContentLevel subLevel : level.getSubLevelList()) {
			renderInfo(w, subLevel);
		}
		w.dec();
	}
}

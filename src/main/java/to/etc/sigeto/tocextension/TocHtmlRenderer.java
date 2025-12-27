package to.etc.sigeto.tocextension;

import org.commonmark.ext.heading.anchor.IdGenerator;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final public class TocHtmlRenderer implements NodeRenderer {
	private final HtmlWriter m_htmlWriter;

	private final HtmlNodeRendererContext m_context;

	public TocHtmlRenderer(HtmlNodeRendererContext context) {
		m_htmlWriter = context.getWriter();
		m_context = context;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(Toc.class);
	}

	@Override
	public void render(Node node) {
		Toc toc = (Toc) node;

		//-- Find the root document, then find all headers.
		Node root = findRoot(node);
		List<Heading> headingList = new ArrayList<>();

		root.accept(new AbstractVisitor() {
			@Override public void visit(Heading heading) {
				headingList.add(heading);
				super.visit(heading);
			}
		});

		//-- The 1st heading is the page title; skip that
		if(headingList.size() < 2)
			return;
		headingList.remove(0);

		Hdr[] levels = new Hdr[20];
		levels[0] = new Hdr(null, 0);
		int levelIndex = 0;
		for(Heading heading : headingList) {
			Hdr currentLevel = levels[levelIndex];

			Hdr nh = new Hdr(heading, heading.getLevel());
			int level = heading.getLevel();
			if(level > 10)
				level = 10;
			if(level < currentLevel.getLevel()) {
				if(levelIndex > 0)
					levelIndex--;
			} else if(level == currentLevel.getLevel()) {
				//-- Append at this index
			} else {
				//-- Higher than current -> add
				levelIndex++;
			}
			levels[levelIndex] = nh;
			levels[levelIndex - 1].getChildren().add(nh);
		}

		//-- We now have a tree of levels; render it
		renderTree(levels[0], 0);
	}

	private void renderTree(Hdr level, int depth) {
		m_htmlWriter.tag("ul", Map.of("class", "ui-toc ui-toc-" + depth));
		for(Hdr child : level.getChildren()) {
			renderLevel(child, depth);
		}
		m_htmlWriter.tag("/ul");
	}

	private void renderLevel(Hdr level, int depth) {
		//StringBuilder sb = new StringBuilder();
		//for(int i = 0; i < depth; i++)
		//	sb.append("  ");

		String render = TextContentRenderer.builder().build().render(level.getHeading());
		//sb.append(render);

		//System.out.println(sb.toString());

		m_htmlWriter.tag("li", Map.of());
		m_htmlWriter.tag("a", Map.of("href", "#" + headerName(level.getHeading())));
		m_htmlWriter.text(render);
		m_htmlWriter.tag("/a");
		m_htmlWriter.tag("/li");
		renderTree(level, depth + 1);
	}

	private IdGenerator m_idGenerator = new IdGenerator.Builder().build();

	private String headerName(Heading heading) {
		//-- Concatenate texts
		StringBuilder sb = new StringBuilder();
		heading.accept(new AbstractVisitor() {
			@Override
			public void visit(Text text) {
				sb.append(text.getLiteral());
			}

			@Override
			public void visit(Code code) {
				sb.append(code.getLiteral());
			}
		});
		String str = sb.toString().trim().toLowerCase();
		String id = m_idGenerator.generateId(str);
		return id;
	}

	private class Hdr {
		private final Heading m_heading;

		private final int m_level;

		private final List<Hdr> m_children = new ArrayList<>(5);

		public Hdr(Heading heading, int level) {
			m_heading = heading;
			m_level = level;
		}

		public int getLevel() {
			return m_level;
		}

		public List<Hdr> getChildren() {
			return m_children;
		}

		public Heading getHeading() {
			return m_heading;
		}
	}

	private Node findRoot(Node node) {
		while(node.getParent() != null) {
			node = node.getParent();
		}
		return node;
	}

}

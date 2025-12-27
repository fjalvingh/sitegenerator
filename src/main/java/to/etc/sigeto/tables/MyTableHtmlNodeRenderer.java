package to.etc.sigeto.tables;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class MyTableHtmlNodeRenderer implements NodeRenderer { // extends TableNodeRenderer {
	private final HtmlWriter m_htmlWriter;

	private final HtmlNodeRendererContext m_context;

	public MyTableHtmlNodeRenderer(HtmlNodeRendererContext context) {
		m_htmlWriter = context.getWriter();
		m_context = context;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(TableBlock.class, TableHead.class, TableBody.class, TableRow.class, TableCell.class);
	}

	@Override
	public void render(Node node) {
		if(node instanceof TableBlock) {
			renderBlock((TableBlock) node);
		} else if(node instanceof TableHead) {
			renderHead((TableHead) node);
		} else if(node instanceof TableBody) {
			renderBody((TableBody) node);
		} else if(node instanceof TableRow) {
			renderRow((TableRow) node);
		} else if(node instanceof TableCell) {
			renderCell((TableCell) node);
		}

	}

	private void renderBlock(TableBlock tableBlock) {
		m_htmlWriter.line();
		Map<String, String> tmap = new HashMap<>(getAttributes(tableBlock, "table"));
		tmap.put("class", "ui-tbl");
		m_htmlWriter.tag("table", tmap);
		renderChildren(tableBlock);
		m_htmlWriter.tag("/table");
		m_htmlWriter.line();
	}

	private void renderHead(TableHead tableHead) {
		m_htmlWriter.line();
		m_htmlWriter.tag("thead", getAttributes(tableHead, "thead"));
		renderChildren(tableHead);
		m_htmlWriter.tag("/thead");
		m_htmlWriter.line();
	}

	private void renderBody(TableBody tableBody) {
		m_htmlWriter.line();
		m_htmlWriter.tag("tbody", getAttributes(tableBody, "tbody"));
		renderChildren(tableBody);
		m_htmlWriter.tag("/tbody");
		m_htmlWriter.line();
	}

	private void renderRow(TableRow tableRow) {
		m_htmlWriter.line();
		m_htmlWriter.tag("tr", getAttributes(tableRow, "tr"));
		renderChildren(tableRow);
		m_htmlWriter.tag("/tr");
		m_htmlWriter.line();
	}

	private void renderCell(TableCell tableCell) {
		String tagName = tableCell.isHeader() ? "th" : "td";
		m_htmlWriter.line();
		m_htmlWriter.tag(tagName, getCellAttributes(tableCell, tagName));
		renderChildren(tableCell);
		m_htmlWriter.tag("/" + tagName);
		m_htmlWriter.line();
	}

	private Map<String, String> getAttributes(Node node, String tagName) {
		return m_context.extendAttributes(node, tagName, Map.of());
	}

	private Map<String, String> getCellAttributes(TableCell tableCell, String tagName) {
		return tableCell.getAlignment() != null ? m_context.extendAttributes(tableCell, tagName, Map.of("align", getAlignValue(tableCell.getAlignment()))) :
			m_context.extendAttributes(tableCell, tagName, Map.of());
	}

	private static String getAlignValue(TableCell.Alignment alignment) {
		switch(alignment){
			case LEFT:
				return "left";
			case CENTER:
				return "center";
			case RIGHT:
				return "right";
			default:
				throw new IllegalStateException("Unknown alignment: " + String.valueOf(alignment));
		}
	}

	private void renderChildren(Node parent) {
		Node next;
		for(Node node = parent.getFirstChild(); node != null; node = next) {
			next = node.getNext();
			m_context.render(node);
		}
	}

}

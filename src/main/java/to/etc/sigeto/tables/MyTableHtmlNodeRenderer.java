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
	private final HtmlWriter htmlWriter;

	private final HtmlNodeRendererContext context;

	public MyTableHtmlNodeRenderer(HtmlNodeRendererContext context) {
		this.htmlWriter = context.getWriter();
		this.context = context;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(TableBlock.class, TableHead.class, TableBody.class, TableRow.class, TableCell.class);
	}

	@Override
	public void render(Node node) {
		if(node instanceof TableBlock) {
			this.renderBlock((TableBlock) node);
		} else if(node instanceof TableHead) {
			this.renderHead((TableHead) node);
		} else if(node instanceof TableBody) {
			this.renderBody((TableBody) node);
		} else if(node instanceof TableRow) {
			this.renderRow((TableRow) node);
		} else if(node instanceof TableCell) {
			this.renderCell((TableCell) node);
		}

	}

	private void renderBlock(TableBlock tableBlock) {
		this.htmlWriter.line();
		Map<String, String> tmap = new HashMap<>(this.getAttributes(tableBlock, "table"));
		tmap.put("class", "ui-tbl");
		this.htmlWriter.tag("table", tmap);
		this.renderChildren(tableBlock);
		this.htmlWriter.tag("/table");
		this.htmlWriter.line();
	}

	private void renderHead(TableHead tableHead) {
		this.htmlWriter.line();
		this.htmlWriter.tag("thead", this.getAttributes(tableHead, "thead"));
		this.renderChildren(tableHead);
		this.htmlWriter.tag("/thead");
		this.htmlWriter.line();
	}

	private void renderBody(TableBody tableBody) {
		this.htmlWriter.line();
		this.htmlWriter.tag("tbody", this.getAttributes(tableBody, "tbody"));
		this.renderChildren(tableBody);
		this.htmlWriter.tag("/tbody");
		this.htmlWriter.line();
	}

	private void renderRow(TableRow tableRow) {
		this.htmlWriter.line();
		this.htmlWriter.tag("tr", this.getAttributes(tableRow, "tr"));
		this.renderChildren(tableRow);
		this.htmlWriter.tag("/tr");
		this.htmlWriter.line();
	}

	private void renderCell(TableCell tableCell) {
		String tagName = tableCell.isHeader() ? "th" : "td";
		this.htmlWriter.line();
		this.htmlWriter.tag(tagName, this.getCellAttributes(tableCell, tagName));
		this.renderChildren(tableCell);
		this.htmlWriter.tag("/" + tagName);
		this.htmlWriter.line();
	}

	private Map<String, String> getAttributes(Node node, String tagName) {
		return this.context.extendAttributes(node, tagName, Map.of());
	}

	private Map<String, String> getCellAttributes(TableCell tableCell, String tagName) {
		return tableCell.getAlignment() != null ? this.context.extendAttributes(tableCell, tagName, Map.of("align", getAlignValue(tableCell.getAlignment()))) :
			this.context.extendAttributes(tableCell, tagName, Map.of());
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
			this.context.render(node);
		}
	}

}

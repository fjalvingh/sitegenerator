package to.etc.sigeto.plantuml;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.PostProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns every fenced code block that says "plantuml" into a {@link
 * PlantumlBlock}. This happens after the parse rather than during it because a
 * fenced block is exactly what it is - letting commonmark find the fence and
 * its content means the diagram may contain anything that is not the closing
 * fence, backticks and blank lines included.
 */
public class PlantumlPostProcessor implements PostProcessor {
	@Override
	public Node process(Node node) {
		List<FencedCodeBlock> fencedList = new ArrayList<>();
		node.accept(new AbstractVisitor() {
			@Override
			public void visit(FencedCodeBlock fenced) {
				if(PlantumlBlock.isPlantuml(fenced.getInfo())) {
					fencedList.add(fenced);
				}
				super.visit(fenced);
			}
		});

		//-- Replacing them while walking the document would change what is being walked
		for(FencedCodeBlock fenced : fencedList) {
			PlantumlBlock block = PlantumlBlock.create(fenced.getInfo(), fenced.getLiteral());
			block.setSourceSpans(fenced.getSourceSpans());			// So an error can name the line the diagram is on
			fenced.insertBefore(block);
			fenced.unlink();
		}
		return node;
	}
}

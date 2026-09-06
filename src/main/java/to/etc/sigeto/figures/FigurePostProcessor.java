package to.etc.sigeto.figures;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.PostProcessor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns every paragraph that holds nothing but one image into a {@link
 * FigureBlock}. This happens after the parse because whether an image stands
 * on its own is a property of the paragraph around it, which is only known
 * once that paragraph has been parsed; a paragraph with an image and text in
 * it, or with two images side by side, is left alone.
 */
public class FigurePostProcessor implements PostProcessor {
	@Override
	public Node process(Node node) {
		List<Paragraph> paragraphList = new ArrayList<>();
		node.accept(new AbstractVisitor() {
			@Override
			public void visit(Paragraph paragraph) {
				if(null != loneImageOf(paragraph)) {
					paragraphList.add(paragraph);
				}
				super.visit(paragraph);
			}
		});

		//-- Replacing them while walking the document would change what is being walked
		for(Paragraph paragraph : paragraphList) {
			Image image = loneImageOf(paragraph);
			if(null == image) {
				continue;											// Cannot happen: loneImageOf said otherwise above
			}
			String title = image.getTitle();
			FigureBlock block = FigureBlock.create(null == title ? "" : title);
			block.setSourceSpans(paragraph.getSourceSpans());		// So an error can name the line the image is on
			Node child = paragraph.getFirstChild();					// The image, or the link around it
			child.unlink();
			block.appendChild(child);
			paragraph.insertBefore(block);
			paragraph.unlink();
		}
		return node;
	}

	/**
	 * The image a paragraph holds when it holds nothing else: the image itself,
	 * or the image inside a link when the author wrote the "[![alt](img)](target)"
	 * form. Null for every other paragraph, which stays a paragraph.
	 */
	@Nullable
	private static Image loneImageOf(@NonNull Paragraph paragraph) {
		Node child = paragraph.getFirstChild();
		if(null == child || null != child.getNext()) {
			return null;
		}
		if(child instanceof Image) {
			return (Image) child;
		}
		if(child instanceof Link) {
			Node inner = child.getFirstChild();
			if(inner instanceof Image && null == inner.getNext()) {
				return (Image) inner;
			}
		}
		return null;
	}
}

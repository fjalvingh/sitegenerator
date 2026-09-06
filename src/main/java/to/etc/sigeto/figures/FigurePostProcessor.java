package to.etc.sigeto.figures;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.parser.PostProcessor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns every paragraph that holds nothing but images into a {@link
 * FigureBlock}. This happens after the parse because whether an image stands
 * on its own is a property of the paragraph around it, which only exists once
 * that paragraph has been parsed; a paragraph mixing an image with text is
 * left alone.
 *
 * Several images written on consecutive lines are one paragraph, and become
 * one figure with one caption - which is what an author writing them that way
 * means: two scans of the facing pages of a manual are a single illustration,
 * not two.
 */
public class FigurePostProcessor implements PostProcessor {
	@Override
	public Node process(Node node) {
		List<Paragraph> paragraphList = new ArrayList<>();
		node.accept(new AbstractVisitor() {
			@Override
			public void visit(Paragraph paragraph) {
				if(null != imagesOf(paragraph)) {
					paragraphList.add(paragraph);
				}
				super.visit(paragraph);
			}
		});

		//-- Replacing them while walking the document would change what is being walked
		for(Paragraph paragraph : paragraphList) {
			List<Image> imageList = imagesOf(paragraph);
			if(null == imageList) {
				continue;											// Cannot happen: imagesOf said otherwise above
			}
			FigureBlock block = FigureBlock.create(FigureBlock.caption(imageList));
			block.setSourceSpans(paragraph.getSourceSpans());		// So an error can name the line the image is on
			for(Node child : childrenOf(paragraph)) {
				if(child instanceof SoftLineBreak) {
					continue;										// The newline between two images; the figure lays them out
				}
				child.unlink();
				block.appendChild(child);							// The image, or the link around it
			}
			paragraph.insertBefore(block);
			paragraph.unlink();
		}
		return node;
	}

	/**
	 * The images a paragraph holds when it holds nothing else: the images
	 * themselves, or the images inside links when the author wrote the
	 * "[![alt](img)](target)" form, with only the line breaks between them.
	 * Null for every other paragraph, which stays a paragraph.
	 */
	@Nullable
	private static List<Image> imagesOf(@NonNull Paragraph paragraph) {
		List<Image> imageList = new ArrayList<>();
		for(Node child : childrenOf(paragraph)) {
			if(child instanceof SoftLineBreak) {
				continue;
			}
			Image image = imageOf(child);
			if(null == image) {
				return null;									// Something that is not an image: an ordinary paragraph
			}
			imageList.add(image);
		}
		return imageList.isEmpty() ? null : imageList;
	}

	/** The image a node is, or the one a link wraps; null when it is neither. */
	@Nullable
	private static Image imageOf(@NonNull Node node) {
		if(node instanceof Image) {
			return (Image) node;
		}
		if(node instanceof Link) {
			Node inner = node.getFirstChild();
			if(inner instanceof Image && null == inner.getNext()) {
				return (Image) inner;
			}
		}
		return null;
	}

	/** The children of a node, as a list, so they can be relinked while iterating. */
	@NonNull
	private static List<Node> childrenOf(@NonNull Node parent) {
		List<Node> list = new ArrayList<>();
		for(Node child = parent.getFirstChild(); null != child; child = child.getNext()) {
			list.add(child);
		}
		return list;
	}
}

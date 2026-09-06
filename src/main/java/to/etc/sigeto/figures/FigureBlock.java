package to.etc.sigeto.figures;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.Image;
import org.commonmark.node.Text;
import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

/**
 * A paragraph that holds nothing but images, which renders as a
 * &lt;figure&gt; with the alt text below it as the &lt;figcaption&gt; instead
 * of as bare &lt;img&gt;s inside a &lt;p&gt;.
 *
 * <pre>
 *   ![An early model in the 5 1/4" box.](1110-1.png)
 *
 *   ![The error table, codes 17 to 29.](errors1.png)
 *   ![The rest of it: codes 30 and 31.](errors2.png)
 * </pre>
 *
 * The alt text is used twice on purpose: it stays each image's alt attribute
 * for a reader who cannot see it, and it becomes the visible caption. Writing
 * it once is what keeps captions cheap enough to actually add - a separate
 * markdown title for the caption was tried and was simply too much typing.
 * An image with no alt text still becomes a figure and gets the same framing;
 * it just has no caption to show.
 *
 * Images written on consecutive lines are one paragraph and so become one
 * figure: two scans of the facing pages of a manual are a single illustration
 * and get a single frame. Their alt texts are joined into that one caption, so
 * each half can still be written as its own sentence.
 */
public class FigureBlock extends CustomBlock {
	@NonNull
	private final String m_caption;

	private FigureBlock(@NonNull String caption) {
		m_caption = caption;
	}

	@NonNull
	public static FigureBlock create(@NonNull String caption) {
		return new FigureBlock(caption);
	}

	/** What the caption says; empty when no image had alt text, and then none is rendered. */
	@NonNull
	public String getCaption() {
		return m_caption;
	}

	/**
	 * The caption for a figure holding these images: their alt texts, in order,
	 * joined into one piece of prose. Images without alt text contribute
	 * nothing rather than a gap.
	 */
	@NonNull
	public static String caption(@NonNull List<Image> imageList) {
		StringBuilder sb = new StringBuilder();
		for(Image image : imageList) {
			String alt = altText(image).strip();
			if(alt.isEmpty()) {
				continue;
			}
			if(sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(alt);
		}
		return sb.toString();
	}

	/**
	 * Concatenates the Text/Code children of an Image node, which is how commonmark
	 * represents an image's alt text (the "..." in ![...](url)).
	 */
	@NonNull
	public static String altText(@NonNull Image node) {
		StringBuilder sb = new StringBuilder();
		node.accept(new AbstractVisitor() {
			@Override public void visit(Text text) {
				sb.append(text.getLiteral());
			}

			@Override public void visit(Code code) {
				sb.append(code.getLiteral());
			}
		});
		return sb.toString();
	}
}

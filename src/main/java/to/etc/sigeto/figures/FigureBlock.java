package to.etc.sigeto.figures;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.Image;
import org.commonmark.node.Text;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A paragraph that holds nothing but a single image, which renders as a
 * &lt;figure&gt; instead of as a bare &lt;img&gt; inside a &lt;p&gt;.
 *
 * <pre>
 *   ![pdp 11/10](1110-1.png)
 *   ![pdp 11/10](1110-1.png "An early model in the 5 1/4\" box.")
 * </pre>
 *
 * The caption is the image's markdown title - the quoted text after the url -
 * and not its alt text: the two say different things. Alt text names the image
 * for a reader who cannot see it and is usually a label ("board layout"),
 * while a caption is written to be read next to the photo. An image without a
 * title still becomes a figure and gets the same framing; it simply shows no
 * caption.
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

	/** What the caption says; empty when the image had no title, and then none is rendered. */
	@NonNull
	public String getCaption() {
		return m_caption;
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

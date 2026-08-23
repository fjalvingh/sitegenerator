package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The model handed to an optional "redirect.jte" template when generating the
 * page that stands in for a document's old location.
 */
public class RedirectModel {
	@NonNull
	private final String m_targetHref;

	@NonNull
	private final String m_targetTitle;

	public RedirectModel(@NonNull String targetHref, @NonNull String targetTitle) {
		m_targetHref = targetHref;
		m_targetTitle = targetTitle;
	}

	/**
	 * The link to the document's new location, relative to the old location.
	 */
	@NonNull
	public String getTargetHref() {
		return m_targetHref;
	}

	/**
	 * The title of the document in its new location.
	 */
	@NonNull
	public String getTargetTitle() {
		return m_targetTitle;
	}
}

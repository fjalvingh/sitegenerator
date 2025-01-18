package to.etc.sigeto;

public class PageModel {
	private final String m_content;

	private final MarkdownChecker m_markdown;

	private final ContentItem m_item;

	public PageModel(String content, MarkdownChecker markdown, ContentItem item) {
		m_content = content;
		m_markdown = markdown;
		m_item = item;
	}

	public String getContent() {
		return m_content;
	}

	public ContentItem getItem() {
		return m_item;
	}

	public String getTitle() {
		return m_item.getPageTitle() == null ? "Content page" : m_item.getPageTitle();
	}

	public String siteURL(String url) {
		return m_markdown.siteURL(url);
	}
}

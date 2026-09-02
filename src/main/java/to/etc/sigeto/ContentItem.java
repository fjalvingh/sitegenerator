package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContentItem {
	private final File m_file;

	private String m_name;

	private final ContentType m_type;

	private final ContentFileType m_fileType;

	//private final String m_relativePath;

	private String m_pageTitle;

	@NonNull private final Content m_content;

	@NonNull
	private ContentLevel m_level;

	/** Used content items, and each item has a list of relative paths used. */
	private Map<ContentItem, Set<String>> m_usedItemList = new HashMap<>();

	private final Map<String, Object> m_frontMatter = new HashMap<>();

	private final Map<String, ContentTag> m_tagMap = new HashMap<>();

	@Nullable
	private LocalDate m_createdDate;

	@Nullable
	private LocalDate m_modifiedDate;

	public ContentItem(@NonNull Content content, @NonNull ContentLevel level, File file, ContentType type, ContentFileType fileType, String relativePath) {
		m_content = content;
		m_level = level;
		m_file = file;
		m_name = file.getName();
		m_type = type;
		m_fileType = fileType;
	}

	@NonNull
	public ContentLevel getLevel() {
		return m_level;
	}

	@NonNull public Content getContent() {
		return m_content;
	}

	public void moveTo(@NonNull ContentLevel level, @NonNull String newName) {
		if(m_level != level) {
			m_level.getSubItems().remove(this);				// Remove from old level
			level.getSubItems().add(this);						// Add to new level
			m_level = level;
		}
		m_name = newName;
	}

	public String getName() {
		return m_name;
	}

	public void setLevel(@NonNull ContentLevel level) {
		m_level = level;
	}

	public File getFile() {
		return m_file;
	}

	public ContentType getType() {
		return m_type;
	}

	public ContentFileType getFileType() {
		return m_fileType;
	}

	public String getRelativePath() {
		String lp = m_level.getRelativePath();
		return lp.length() == 0 ? getName() : lp + "/" + getName();
	}

	public String getRelativeTargetPath() {
		if(getFileType() == ContentFileType.Markdown) {
			String lp = m_level.getRelativePath();
			String name = Util.getFilenameSansExtension(getName()) + ".html";

			return lp.length() == 0 ? name : lp + "/" + name;
		} else {
			return getRelativePath();
		}
	}

	@Nullable
	public ContentItem findItemByURL(String url) {
		String fullPath = resolveURL(url);
		if(null == fullPath)
			return null;
		return getContent().findItem(fullPath);
	}

	/**
	 * Resolve a link or image url used on this page into the content-root
	 * relative path it addresses, in the form the content map is keyed by.
	 * Returns null for urls that do not address content at all (external
	 * links, in-page anchors).
	 *
	 * <p>A "#fragment" addresses a place inside the document rather than
	 * another document, so it takes no part in resolving: it is removed here
	 * and checked separately (see {@link MarkdownChecker}).</p>
	 */
	@Nullable
	public String resolveURL(String url) {
		if(!Content.isRelativePath(url))
			return null;
		url = Content.documentPart(url);
		if(url.isEmpty())									// "#fragment" only: this page itself
			return null;

		if(url.startsWith("/")) {
			return url.substring(1);
		}

		//-- Relative wrt the parent
		Path path = Path.of(getDirectoryPath());
		Path resolvedPath = path.resolve(url).normalize();
		return resolvedPath.toString();
	}

	/*----------------------------------------------------------------------*/
	/*	CODING:	The anchors this page will have						*/
	/*----------------------------------------------------------------------*/

	/**
	 * Every id this page renders: the anchors of its headings, plus the ids
	 * written in raw html in the markdown itself. A link ending in "#name" is
	 * checked against this, so a link into a section that is not there fails
	 * the build just like a link to a document that is not there.
	 */
	private final Set<String> m_anchorSet = new HashSet<>();

	public void addAnchor(String anchor) {
		m_anchorSet.add(anchor);
	}

	public boolean hasAnchor(String anchor) {
		return m_anchorSet.contains(anchor);
	}

	public Set<String> getAnchorSet() {
		return m_anchorSet;
	}


	public String getDirectoryPath() {
		return m_level.getRelativePath();
	}

	public String getPageTitle() {
		return m_pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		m_pageTitle = pageTitle;
	}

	public void addUsedItem(ContentItem item, String relativePath) {
		m_usedItemList.computeIfAbsent(item, a -> new HashSet<>()).add(relativePath);
	}

	public Map<ContentItem, Set<String>> getUsedItemList() {
		return m_usedItemList;
	}

	/**
	 * Return T if this is inside the level or a sublevel of that level.
	 */
	public boolean isInside(ContentLevel level) {
		return m_level.isInside(level);
	}

	@Override public String toString() {
		return getRelativePath() + " [" + m_fileType + "]";
	}

	public void setFrontMatter(Map<String, Object> map) {
		m_frontMatter.putAll(map);
	}

	/**
	 * The date this file was first added, taken from its git history if the
	 * file is inside a git repository, else the file's own last-modified date.
	 */
	@NonNull
	public LocalDate getCreatedDate() {
		resolveDates();
		LocalDate date = m_createdDate;
		assert date != null;
		return date;
	}

	/**
	 * The date this file was last changed, taken from its git history if the
	 * file is inside a git repository, else the file's own last-modified date.
	 */
	@NonNull
	public LocalDate getModifiedDate() {
		resolveDates();
		LocalDate date = m_modifiedDate;
		assert date != null;
		return date;
	}

	private void resolveDates() {
		if(null == m_createdDate) {
			GitDateUtil.DateInfo info = GitDateUtil.getDates(m_file);
			m_createdDate = info.created;
			m_modifiedDate = info.modified;
		}
	}

	public Map<String, Object> getFrontMatter() {
		return m_frontMatter;
	}
}

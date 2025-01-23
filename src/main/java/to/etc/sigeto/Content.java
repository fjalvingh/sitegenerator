package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Content {
	/** The map of relative path to item */
	private Map<String, ContentItem> m_itemMap = new HashMap<String, ContentItem>();

	private int m_markDownItemCount;

	/** For MarkDown items this is the rendering of the file, after all checks. */
	private String m_renderedContent;

	private Set<ContentItem> m_usedResourceList = new HashSet<>();

	private ContentLevel m_pageRootLevel;

	@Nullable
	private ContentLevel m_blogRootLevel;

	public static Content create(File root) {
		StringBuilder sb = new StringBuilder();
		Content content = new Content();

		ContentLevel rootLevel = content.scanContent(sb, null, root, ContentType.Page);
		if(null == rootLevel)
			throw new MessageException("No content inside the content directory");
		content.m_pageRootLevel = rootLevel;

		//-- For the root level the page starts at index.md
		for(ContentItem subItem : rootLevel.getSubItems()) {
			if(subItem.getFileType() == ContentFileType.Markdown && subItem.getRelativePath().startsWith("index")) {
				rootLevel.setRootItem(subItem);
				break;
			}
		}
		return content;
	}

	@Nullable
	private ContentLevel scanContent(StringBuilder sb, @Nullable ContentLevel parentLevel, File root, ContentType type) {
		int len = sb.length();
		ContentLevel level = new ContentLevel(root, sb.toString(), type, parentLevel);

		if(root.getName().equalsIgnoreCase("blogs")) {
			type = ContentType.Blog;
			if(m_blogRootLevel == null)
				m_blogRootLevel = level;
		}

		File[] files = root.listFiles();
		if(null == files || files.length == 0) {
			return null;
		}
		for(File file : files) {
			sb.setLength(len);
			if(len > 0)
				sb.append('/');
			sb.append(file.getName());
			if(file.isFile()) {
				String relative = sb.toString();
				ContentItem ci = new ContentItem(level, file, type, getType(file), relative);
				if(ci.getFileType() == ContentFileType.Markdown) {
					m_markDownItemCount++;
				}
				m_itemMap.put(relative, ci);
				level.addItem(ci);

				////-- Is this the index.md file?
				//if(ci.getFileType() == ContentFileType.Markdown && file.getName().toLowerCase().startsWith("index.")) {
				//	level.setRootItem(ci);
				//}
			} else if(file.isDirectory()) {
				ContentLevel contentLevel = scanContent(sb, level, file, type);
				if(null != contentLevel) {
					level.addSubLevel(contentLevel);

					//-- Do we have an index for this thing?
				}
			}
		}

		//-- For every sublevel found here: is there an index page for it here too?
		//if(level.getParentLevel() != null) {
			for(ContentLevel sub : level.getSubLevelList()) {
				if(sub.hasMarkdown()) {
					ContentItem item = level.findItemByName(sub.getName() + ".md");
					if(null == item) {
						item = level.findItemByName(sub.getName() + ".mdown");
					}
					if(null != item) {
						sub.setRootItem(item);
					} else {
						System.out.println(sub.getRelativePath() + ": no index page found for this sublevel");
					}
				}
			}
		//}

		return level.getSubItems().isEmpty() && level.getSubLevelList().isEmpty() ? null : level;
	}

	private ContentFileType getType(File file) {
		String name = file.getName();
		int pos = name.lastIndexOf(".");
		if(pos == -1) {
			return ContentFileType.Resource;
		}
		String ext = name.substring(pos + 1).toLowerCase();
		switch(ext){
			default:
				return ContentFileType.Resource;

			case "png":
			case "gif":
			case "jpg":
			case "jpeg":
				return ContentFileType.Image;

			case "md":
			case "mdown":
				return ContentFileType.Markdown;
		}
	}

	static public boolean isRelativePath(String url) {
		if(url.indexOf(':') != -1)                // http(s): url?
			return false;                        // We cannot check those currently
		if(url.startsWith("#"))
			return false;
		return true;
	}

	public List<ContentItem> getItemList() {
		return new ArrayList<ContentItem>(m_itemMap.values());
	}

	public int getMarkDownItemCount() {
		return m_markDownItemCount;
	}

	@Nullable
	public ContentItem findItem(String fullPath) {
		return m_itemMap.get(fullPath);
	}

	public String getRenderedContent() {
		return m_renderedContent;
	}

	public void setRenderedContent(String renderedContent) {
		m_renderedContent = renderedContent;
	}

	void appendUsedResource(ContentItem item) {
		m_usedResourceList.add(item);
	}

	public Set<ContentItem> getUsedResourceList() {
		return m_usedResourceList;
	}

	@NonNull
	public ContentLevel getPageRootLevel() {
		return m_pageRootLevel;
	}
}

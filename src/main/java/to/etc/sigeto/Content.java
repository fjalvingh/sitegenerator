package to.etc.sigeto;

import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Content {
	/** The map of relative path to item */
	private Map<String, ContentItem> m_itemMap = new HashMap<String, ContentItem>();

	private int m_markDownItemCount;

	/** For MarkDown items this is the rendering of the file, after all checks. */
	private String m_renderedContent;

	public static Content create(File root) {
		StringBuilder sb = new StringBuilder();
		Content content = new Content();
		content.scanContent(content, sb, root);
		return content;
	}

	private void scanContent(Content content, StringBuilder sb, File root) {
		int len = sb.length();
		for(File file : root.listFiles()) {
			sb.setLength(len);
			sb.append("/").append(file.getName());
			if(file.isFile()) {
				String relative = sb.toString().substring(1);
				ContentItem ci = new ContentItem(file, getType(file), relative);
				if(ci.getType() == ContentType.Markdown) {
					m_markDownItemCount++;
				}
				m_itemMap.put(relative, ci);
			} else if(file.isDirectory()) {
				scanContent(content, sb, file);
			}
		}
	}

	private ContentType getType(File file) {
		String name = file.getName();
		int pos = name.lastIndexOf(".");
		if(pos == -1) {
			return ContentType.Resource;
		}
		String ext = name.substring(pos + 1).toLowerCase();
		switch(ext){
			default:
				return ContentType.Resource;

			case "png":
			case "gif":
			case "jpg":
			case "jpeg":
				return ContentType.Image;

			case "md":
			case "mdown":
				return ContentType.Markdown;
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
}

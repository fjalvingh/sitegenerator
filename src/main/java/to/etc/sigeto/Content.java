package to.etc.sigeto;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Content {
	/** The map of relative path to item */
	private Map<String, ContentItem> m_itemMap = new HashMap<String, ContentItem>();

	private int m_markDownItemCount;

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
				String relative = sb.toString();
				ContentItem ci = new ContentItem(file, getType(file), relative.substring(1));
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
		switch(ext) {
			default:
				return ContentType.Resource;

			case "md":
			case "mdown":
				return ContentType.Markdown;
		}
	}

	public int getMarkDownItemCount() {
		return m_markDownItemCount;
	}
}

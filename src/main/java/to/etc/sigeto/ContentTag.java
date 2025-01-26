package to.etc.sigeto;

import java.util.ArrayList;
import java.util.List;

public class ContentTag {
	private final String m_tag;

	private final List<ContentItem> m_itemList = new ArrayList<>();

	public ContentTag(String tag) {
		m_tag = tag;
	}

	void addItem(ContentItem item) {
		m_itemList.add(item);
	}

	public List<ContentItem> getItemList() {
		return m_itemList;
	}
}

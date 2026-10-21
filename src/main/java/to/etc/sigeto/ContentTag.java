package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A tag, as used in the "tags:" front matter of one or more pages. Tags are
 * case insensitive: "API" and "api" are the same tag, shown with the spelling
 * most pages use.
 */
final public class ContentTag {
	/** The order tags are shown in: alphabetically, ignoring case. */
	public static final Comparator<ContentTag> BY_NAME = Comparator.comparing((ContentTag a) -> a.getName().toLowerCase())
		.thenComparing(ContentTag::getName);

	/** The tag's identity: its name in lower case. */
	@NonNull
	private final String m_key;

	/** Every way the tag is written, with the pages writing it that way. Sorted, so that ties are decided the same way every build. */
	private final Map<String, List<ContentItem>> m_spellingMap = new TreeMap<>();

	private final List<ContentItem> m_itemList = new ArrayList<>();

	/** The id of this tag's section on the tag page, assigned by {@link Content#complete()}. */
	@Nullable
	private String m_anchor;

	ContentTag(@NonNull String key) {
		m_key = key;
	}

	/**
	 * Tag the page, written the way the page writes it. Called once per page,
	 * see {@link ContentItem#addTag(ContentTag)}.
	 */
	void addItem(@NonNull ContentItem item, @NonNull String spelling) {
		m_itemList.add(item);
		m_spellingMap.computeIfAbsent(spelling, a -> new ArrayList<>()).add(item);
	}

	@NonNull
	public String getKey() {
		return m_key;
	}

	/**
	 * The name the tag is shown with: the spelling most of its pages use.
	 */
	@NonNull
	public String getName() {
		String best = m_key;
		int bestCount = 0;
		for(Map.Entry<String, List<ContentItem>> entry : m_spellingMap.entrySet()) {
			if(entry.getValue().size() > bestCount) {
				bestCount = entry.getValue().size();
				best = entry.getKey();
			}
		}
		return best;
	}

	/**
	 * Every way pages write this tag, with the pages writing it that way.
	 */
	@NonNull
	Map<String, List<ContentItem>> getSpellingMap() {
		return m_spellingMap;
	}

	/**
	 * The pages having this tag, in the order they were found.
	 */
	@NonNull
	public List<ContentItem> getItemList() {
		return m_itemList;
	}

	/**
	 * The id of this tag's section on the tag page.
	 */
	@NonNull
	public String getAnchor() {
		String anchor = m_anchor;
		if(null == anchor)
			throw new IllegalStateException("Tag anchors are only known after Content.complete()");
		return anchor;
	}

	void setAnchor(@NonNull String anchor) {
		m_anchor = anchor;
	}

	@Override
	public String toString() {
		return getName();
	}
}

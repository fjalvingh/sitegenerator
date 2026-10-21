package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The page the generator makes when pages have tags: a tag cloud linking to a
 * section per tag, each listing the pages that have that tag. It is rendered
 * through base.jte like any other page, and is the last item of the top level
 * of the menu.
 */
final class TagPage {
	/** Where the tag page is written, relative to the site root. */
	static final String FILE_NAME = "tags.html";

	static final String TITLE = "Tags";

	/** The number of sizes a tag can have in the cloud: ui-tag-w1 (used least) to ui-tag-w5 (used most). */
	static final int WEIGHT_COUNT = 5;

	/** The order of the pages listed under a tag: by title, ignoring case. */
	private static final Comparator<ContentItem> BY_TITLE = Comparator.comparing((ContentItem a) -> titleOf(a).toLowerCase())
		.thenComparing(ContentItem::getRelativePath);

	private TagPage() {
	}

	/**
	 * Give every tag the id of its section on the tag page: its name, made fit
	 * for an id, and numbered when two names come out the same.
	 */
	static void assignAnchors(@NonNull List<ContentTag> tagList) {
		Set<String> usedSet = new HashSet<>();
		for(ContentTag tag : tagList) {
			String base = "tag-" + slug(tag.getKey());
			String anchor = base;
			for(int i = 2; !usedSet.add(anchor); i++) {
				anchor = base + "-" + i;
			}
			tag.setAnchor(anchor);
		}
	}

	/**
	 * Lower case letters and digits, with a single dash for everything in
	 * between them.
	 */
	@NonNull
	private static String slug(@NonNull String name) {
		StringBuilder sb = new StringBuilder();
		boolean dash = false;
		for(int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if(Character.isLetterOrDigit(c)) {
				if(dash && !sb.isEmpty())
					sb.append('-');
				sb.append(Character.toLowerCase(c));
				dash = false;
			} else {
				dash = true;
			}
		}
		return sb.toString();
	}

	/**
	 * Create the item for the tag page, at the root of the site. It dates from
	 * the oldest tagged page, and was last changed when a tagged page last was.
	 */
	@NonNull
	static ContentItem createItem(@NonNull Content content, @NonNull List<ContentTag> tagList) {
		for(ContentItem item : content.getItemList()) {
			if(item.getRelativeTargetPath().equals(FILE_NAME))
				throw new MessageException(item.getRelativePath() + ": the generator writes the tag page to " + FILE_NAME + ", so a page of its own cannot be there");
		}

		LocalDate created = null;
		LocalDate modified = null;
		for(ContentTag tag : tagList) {
			for(ContentItem item : tag.getItemList()) {
				LocalDate c = item.getCreatedDate();
				LocalDate m = item.getModifiedDate();
				if(null == created || c.isBefore(created))
					created = c;
				if(null == modified || m.isAfter(modified))
					modified = m;
			}
		}
		if(null == created || null == modified)
			throw new IllegalStateException("A tag page needs tagged pages");
		return ContentItem.generated(content, content.getPageRootLevel(), FILE_NAME, TITLE, created, modified);
	}

	/**
	 * The html of the page's content: the title, the cloud, and a section per
	 * tag. The page is at the site root, so the links are site root relative.
	 */
	@NonNull
	static String render(@NonNull List<ContentTag> tagList) {
		int min = Integer.MAX_VALUE;
		int max = 0;
		for(ContentTag tag : tagList) {
			int count = tag.getItemList().size();
			min = Math.min(min, count);
			max = Math.max(max, count);
		}

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<h1>").append(Util.escapeHtml(TITLE)).append("</h1>\n");
		sb.append("<div class=\"ui-tag-cloud\">\n");
		for(ContentTag tag : tagList) {
			int count = tag.getItemList().size();
			sb.append("<a class=\"ui-tag ui-tag-w").append(weight(count, min, max)).append("\" href=\"#").append(Util.escapeHtml(tag.getAnchor()))
				.append("\" title=\"").append(count).append(count == 1 ? " page" : " pages").append("\">")
				.append(Util.escapeHtml(tag.getName()))
				.append("</a>\n");
		}
		sb.append("</div>\n");

		for(ContentTag tag : tagList) {
			sb.append("<h2 class=\"ui-tag-title\" id=\"").append(Util.escapeHtml(tag.getAnchor())).append("\">").append(Util.escapeHtml(tag.getName())).append("</h2>\n");
			sb.append("<ul class=\"ui-tag-pages\">\n");
			List<ContentItem> itemList = new ArrayList<>(tag.getItemList());
			itemList.sort(BY_TITLE);
			for(ContentItem item : itemList) {
				sb.append("<li><a href=\"").append(Util.escapeHtml(item.getRelativeTargetPath())).append("\">")
					.append(Util.escapeHtml(titleOf(item)))
					.append("</a></li>\n");
			}
			sb.append("</ul>\n");
		}
		return sb.toString();
	}

	/**
	 * The size of a tag in the cloud, from 1 to {@link #WEIGHT_COUNT}. The scale
	 * is logarithmic, so that one tag used everywhere does not shrink all the
	 * others to the same smallest size.
	 */
	static int weight(int count, int min, int max) {
		if(max <= min)
			return (WEIGHT_COUNT + 1) / 2;
		double fraction = (Math.log(count) - Math.log(min)) / (Math.log(max) - Math.log(min));
		return 1 + (int) Math.round(fraction * (WEIGHT_COUNT - 1));
	}

	@NonNull
	private static String titleOf(@NonNull ContentItem item) {
		String title = item.getPageTitle();
		return null == title || title.isBlank() ? item.getRelativePath() : title;
	}
}

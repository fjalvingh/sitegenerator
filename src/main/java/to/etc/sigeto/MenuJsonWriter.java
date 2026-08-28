package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes the site menu as a json file at the root of the generated site, so
 * that a page can build its menu in the browser instead of having a copy of it
 * generated into the page itself. Both are always available; which one a site
 * uses is up to its templates.
 *
 * The links in it are relative to the site root; a page turns them into real
 * links by prefixing them with its own path back to that root.
 */
final class MenuJsonWriter {
	static final String FILE_NAME = "menu.json";

	private MenuJsonWriter() {
	}

	static void write(@NonNull File outputRoot, @NonNull Menu menu) throws Exception {
		StringBuilder sb = new StringBuilder(8192);
		sb.append("{\n  \"items\": ");
		appendItems(sb, menu.getRootItemList(), 1);
		sb.append("\n}\n");
		Util.writeFileFromString(new File(outputRoot, FILE_NAME), sb.toString(), StandardCharsets.UTF_8);
	}

	private static void appendItems(@NonNull StringBuilder sb, @NonNull List<MenuItem> itemList, int depth) {
		if(itemList.isEmpty()) {
			sb.append("[]");
			return;
		}
		String indent = "  ".repeat(depth + 1);
		sb.append("[");
		for(int i = 0; i < itemList.size(); i++) {
			MenuItem item = itemList.get(i);
			if(i > 0) {
				sb.append(",");
			}
			sb.append("\n").append(indent).append("{\"title\": ");
			appendString(sb, item.getTitle());
			sb.append(", \"href\": ");
			appendString(sb, item.getTargetPath());
			if(item.hasChildren()) {
				sb.append(", \"items\": ");
				appendItems(sb, item.getSubItemList(), depth + 1);
			}
			sb.append("}");
		}
		sb.append("\n").append("  ".repeat(depth)).append("]");
	}

	private static void appendString(@NonNull StringBuilder sb, @NonNull String in) {
		sb.append('"');
		for(int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			switch(c){
				default:
					if(c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
					break;

				case '"':
					sb.append("\\\"");
					break;

				case '\\':
					sb.append("\\\\");
					break;

				case '\n':
					sb.append("\\n");
					break;

				case '\r':
					sb.append("\\r");
					break;

				case '\t':
					sb.append("\\t");
					break;
			}
		}
		sb.append('"');
	}
}

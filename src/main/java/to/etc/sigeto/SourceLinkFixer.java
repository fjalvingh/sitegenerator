package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repairs links to moved documents in the markdown sources themselves. The
 * generated site keeps old URLs working through redirect pages, but the site's
 * own sources should point at where the document lives now - so they are
 * rewritten in place, and the build then stops so the changes can be reviewed
 * and committed before the site is published.
 */
final class SourceLinkFixer {
	private SourceLinkFixer() {
	}

	/**
	 * Apply all collected fixes, rewriting each source file at most once.
	 * Returns the number of links that were actually changed; anything that
	 * could not be located in the source is reported as a warning instead so
	 * it can be fixed by hand.
	 */
	static int apply(@NonNull List<LinkFix> fixList, @NonNull List<Message> errorList) throws Exception {
		//-- Group by file, keeping the order the fixes were found in
		Map<ContentItem, List<LinkFix>> perFile = new LinkedHashMap<>();
		for(LinkFix fix : fixList) {
			perFile.computeIfAbsent(fix.getItem(), a -> new ArrayList<>()).add(fix);
		}

		int fixedCount = 0;
		int fileCount = 0;
		for(Map.Entry<ContentItem, List<LinkFix>> entry : perFile.entrySet()) {
			int fixed = fixFile(entry.getKey(), entry.getValue(), errorList);
			if(fixed > 0) {
				fixedCount += fixed;
				fileCount++;
			}
		}
		if(fixedCount > 0) {
			System.out.println("Fixed " + fixedCount + " stale link(s) in " + fileCount + " file(s) - please review and commit them");
		}
		return fixedCount;
	}

	private static int fixFile(@NonNull ContentItem item, @NonNull List<LinkFix> fixList, @NonNull List<Message> errorList) throws Exception {
		File file = item.getFile();
		String text;
		try {
			text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		} catch(IOException x) {
			errorList.add(new Message(item, 0, MsgType.Error, "Cannot read this file to fix its links: " + x));
			return 0;
		}

		List<String> lines = splitLines(text);
		int fixed = 0;
		for(LinkFix fix : fixList) {
			if(replace(lines, fix)) {
				fixed++;
			} else {
				errorList.add(new Message(item, fix.getLineNumber(), MsgType.Warning,
					"Could not rewrite the link to " + fix.getOldUrl() + " automatically - please change it to " + fix.getNewUrl() + " by hand"));
			}
		}
		if(fixed == 0)
			return 0;

		StringBuilder sb = new StringBuilder(text.length() + 128);
		for(String line : lines) {
			sb.append(line);
		}
		Util.writeFileFromString(file, sb.toString(), StandardCharsets.UTF_8);
		return fixed;
	}

	/**
	 * Replace the link destination, preferring the line the parser reported it
	 * on and falling back to a scan of the whole file for links spanning more
	 * than one line.
	 */
	private static boolean replace(@NonNull List<String> lines, @NonNull LinkFix fix) {
		int index = fix.getLineNumber() - 1;
		if(index >= 0 && index < lines.size() && replaceInLine(lines, index, fix))
			return true;
		for(int i = 0; i < lines.size(); i++) {
			if(i != index && replaceInLine(lines, i, fix))
				return true;
		}
		return false;
	}

	/**
	 * Replace the destination only where it is actually used as one - inside
	 * "](url)", "](url "title")", "](&lt;url&gt;)", a "[ref]: url" definition or
	 * an autolink - so that the same text elsewhere in the line (prose, inline
	 * code) is left alone.
	 */
	private static boolean replaceInLine(@NonNull List<String> lines, int index, @NonNull LinkFix fix) {
		String line = lines.get(index);
		String oldUrl = fix.getOldUrl();
		String newUrl = fix.getNewUrl();

		String[][] forms = {
			{"](" + oldUrl + ")", "](" + newUrl + ")"},
			{"](" + oldUrl + " ", "](" + newUrl + " "},
			{"](" + oldUrl + "\t", "](" + newUrl + "\t"},
			{"](<" + oldUrl + ">", "](<" + newUrl + ">"},
			{"]: " + oldUrl, "]: " + newUrl},
			{"]:\t" + oldUrl, "]:\t" + newUrl},
			{"<" + oldUrl + ">", "<" + newUrl + ">"},
		};

		for(String[] form : forms) {
			int pos = line.indexOf(form[0]);
			if(pos >= 0) {
				lines.set(index, line.substring(0, pos) + form[1] + line.substring(pos + form[0].length()));
				return true;
			}
		}
		return false;
	}

	/**
	 * Split into lines, each keeping its own line terminator, so that
	 * reassembling the file preserves its line endings and its (possibly
	 * missing) final newline exactly.
	 */
	@NonNull
	private static List<String> splitLines(@NonNull String text) {
		List<String> lines = new ArrayList<>();
		int start = 0;
		for(int i = 0; i < text.length(); i++) {
			if(text.charAt(i) == '\n') {
				lines.add(text.substring(start, i + 1));
				start = i + 1;
			}
		}
		if(start < text.length()) {
			lines.add(text.substring(start));
		}
		return lines;
	}
}

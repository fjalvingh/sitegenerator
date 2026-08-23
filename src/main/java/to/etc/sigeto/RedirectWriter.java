package to.etc.sigeto;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import org.eclipse.jdt.annotation.NonNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Writes a small page at every location a document used to have, sending both
 * browsers and search engines on to where that document lives now. This keeps
 * links that were shared before the move - and which cannot be fixed, because
 * they live on other people's sites - working.
 *
 * A meta refresh page is used because it needs no server configuration at all,
 * so it works on plain static hosting like Github Pages.
 */
final class RedirectWriter {
	/** The template used when the site provides one; otherwise a built-in page is written. */
	private static final String TEMPLATE = "redirect.jte";

	private RedirectWriter() {
	}

	/**
	 * Generate a redirect page for every known move whose old location is a
	 * markdown document that is no longer there. Returns the number of pages
	 * written.
	 */
	static int write(@NonNull File outputRoot, @NonNull File templateRoot, @NonNull TemplateEngine templateEngine, @NonNull Content content, @NonNull MoveMap moveMap) throws Exception {
		boolean hasTemplate = new File(templateRoot, TEMPLATE).exists();
		int count = 0;
		for(Map.Entry<String, String> entry : moveMap.getUsableMoves().entrySet()) {
			String oldPath = entry.getKey();
			if(!isMarkdown(oldPath))									// Only pages have an URL that a redirect page can serve
				continue;
			ContentItem target = content.findItem(entry.getValue());
			if(null == target)											// Already reported by MoveMap.resolve
				continue;

			if(writePage(outputRoot, templateEngine, hasTemplate, targetPath(oldPath), target.getRelativeTargetPath(), target)) {
				count++;
			}

			//-- A blog entry is also published in the sitewide timeline, so its old timeline url needs a redirect too
			String globalPath = target.getLevel().getGlobalOutputPath();
			if(isBlogPath(oldPath) && null != globalPath) {
				if(writePage(outputRoot, templateEngine, hasTemplate, "blog-timeline/" + targetPath(oldPath), globalPath, target)) {
					count++;
				}
			}
		}
		if(count > 0) {
			System.out.println("Wrote " + count + " redirect page(s) for moved documents");
		}
		return count;
	}

	/**
	 * Write one redirect page, unless the site already has a real page there.
	 */
	private static boolean writePage(@NonNull File outputRoot, @NonNull TemplateEngine templateEngine, boolean hasTemplate, @NonNull String oldTarget, @NonNull String newTarget, @NonNull ContentItem target) throws Exception {
		File out = new File(outputRoot, oldTarget);
		if(out.exists())												// Never overwrite real content
			return false;
		File parentFile = out.getParentFile();
		if(null != parentFile) {
			parentFile.mkdirs();
		}

		String href = Util.relativeHref(directoryOf(oldTarget), newTarget);
		String title = target.getPageTitle();
		if(null == title) {
			title = "its new location";
		}

		String page;
		if(hasTemplate) {
			TemplateOutput output = new StringOutput(4096);
			templateEngine.render(TEMPLATE, new RedirectModel(href, title), output);
			page = output.toString();
		} else {
			page = defaultPage(href, title);
		}
		Util.writeFileFromString(out, page, StandardCharsets.UTF_8);
		return true;
	}

	@NonNull
	private static String defaultPage(@NonNull String href, @NonNull String title) {
		String escapedHref = escape(href);
		return "<!doctype html>\n"
			+ "<html lang=\"en\">\n"
			+ "<head>\n"
			+ "<meta charset=\"utf-8\">\n"
			+ "<title>Page moved</title>\n"
			+ "<link rel=\"canonical\" href=\"" + escapedHref + "\">\n"
			+ "<meta http-equiv=\"refresh\" content=\"0; url=" + escapedHref + "\">\n"
			+ "<meta name=\"robots\" content=\"noindex\">\n"
			+ "</head>\n"
			+ "<body>\n"
			+ "<p>This page has moved to <a href=\"" + escapedHref + "\">" + escape(title) + "</a>.</p>\n"
			+ "</body>\n"
			+ "</html>\n";
	}

	/**
	 * The output path a markdown source path is generated to, the same way
	 * {@link ContentItem#getRelativeTargetPath()} does it.
	 */
	@NonNull
	private static String targetPath(@NonNull String sourcePath) {
		return Util.getFilenameSansExtension(sourcePath) + ".html";
	}

	@NonNull
	private static String directoryOf(@NonNull String path) {
		int slash = path.lastIndexOf('/');
		return slash < 0 ? "" : path.substring(0, slash);
	}

	/**
	 * T if this path was inside a blog (yyyymmdd) directory, and so was also
	 * published in the sitewide blog timeline.
	 */
	private static boolean isBlogPath(@NonNull String path) {
		String directory = directoryOf(path);
		int slash = directory.lastIndexOf('/');
		String name = slash < 0 ? directory : directory.substring(slash + 1);
		if(name.length() != 8)
			return false;
		for(int i = 0; i < 8; i++) {
			if(!Character.isDigit(name.charAt(i)))
				return false;
		}
		return true;
	}

	private static boolean isMarkdown(@NonNull String path) {
		String extension = Util.getExtension(path).toLowerCase();
		return "md".equals(extension) || "mdown".equals(extension);
	}

	@NonNull
	private static String escape(@NonNull String in) {
		return in
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}
}

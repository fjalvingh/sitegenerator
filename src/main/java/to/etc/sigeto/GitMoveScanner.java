package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.sigeto.utils.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Finds document moves by asking git for the renames it detected inside the
 * content root, over the entire history, oldest commit first. Used to keep the
 * site's redirect map up-to-date without any manual bookkeeping.
 *
 * As with {@link GitDateUtil} this shells out to the git command instead of
 * adding a git library dependency. Anything that goes wrong - git missing, the
 * content not being inside a repository, the content not being tracked (e.g.
 * the gitignored testsite/ example) - just yields an empty list, leaving the
 * checked-in redirect map as the only source of moves.
 */
final class GitMoveScanner {
	/** Renames are detected over the whole history, so allow a lot more time than a per-file query. */
	private static final int TIMEOUT_SECONDS = 30;

	private GitMoveScanner() {
	}

	/**
	 * Return all renames git detected below the content root, as
	 * (oldPath, newPath) pairs relative to that content root, in chronological
	 * (oldest first) order so that chained moves can be collapsed by replaying
	 * them in sequence.
	 */
	@NonNull
	static List<Pair<String, String>> scanRenames(@NonNull File contentRoot) {
		String prefix = runGit(contentRoot, "rev-parse", "--show-prefix");
		if(null == prefix)                                    // Not a repository, or no git at all
			return Collections.emptyList();
		prefix = prefix.trim();                                // "site/content/", or "" when content IS the repo root

		String log = runGit(contentRoot, "log", "--reverse", "--diff-filter=R", "-M", "--name-status", "-z", "--format=", "--", ".");
		if(null == log)
			return Collections.emptyList();
		return parseRenames(log, prefix);
	}

	/**
	 * Parse the NUL separated output of "git log --name-status -z". Rename
	 * records are three consecutive fields: the status ("R100"), the old path
	 * and the new path, all relative to the repository root. Any other field
	 * is skipped, which also takes care of the empty records the (suppressed)
	 * commit header leaves behind.
	 */
	@NonNull
	static List<Pair<String, String>> parseRenames(@NonNull String output, @NonNull String prefix) {
		List<Pair<String, String>> result = new ArrayList<>();
		List<String> fields = splitOnNul(output);
		for(int i = 0; i < fields.size(); i++) {
			String field = fields.get(i);
			if(!field.startsWith("R") || i + 2 >= fields.size())
				continue;
			String oldPath = stripPrefix(fields.get(i + 1), prefix);
			String newPath = stripPrefix(fields.get(i + 2), prefix);
			i += 2;
			if(null == oldPath || null == newPath)            // Moved in or out of the content root: not a site move
				continue;
			if(oldPath.equals(newPath))
				continue;
			result.add(new Pair<>(oldPath, newPath));
		}
		return result;
	}

	/**
	 * Split the NUL separated output into its fields.
	 */
	@NonNull
	private static List<String> splitOnNul(@NonNull String output) {
		List<String> fields = new ArrayList<>();
		int start = 0;
		for(int i = 0; i < output.length(); i++) {
			if(output.charAt(i) == 0) {
				fields.add(output.substring(start, i));
				start = i + 1;
			}
		}
		if(start < output.length()) {
			fields.add(output.substring(start));
		}
		return fields;
	}

	/**
	 * Make a repository-relative path relative to the content root, or return
	 * null if the path is not inside the content root at all.
	 */
	@Nullable
	private static String stripPrefix(@NonNull String path, @NonNull String prefix) {
		if(prefix.isEmpty())
			return path;
		if(!path.startsWith(prefix))
			return null;
		String stripped = path.substring(prefix.length());
		return stripped.isEmpty() ? null : stripped;
	}

	/**
	 * Run a git command inside the given directory, returning its stdout, or
	 * null if git could not be run or exited with an error.
	 */
	@Nullable
	private static String runGit(@NonNull File directory, @NonNull String... arguments) {
		if(!directory.exists())
			return null;
		List<String> command = new ArrayList<>();
		command.add("git");
		Collections.addAll(command, arguments);
		try {
			Process process = new ProcessBuilder(command)
				.directory(directory)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();

			String stdout = readFully(process.getInputStream());
			boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if(!finished) {
				process.destroyForcibly();
				return null;
			}
			if(process.exitValue() != 0)
				return null;
			return stdout;
		} catch(IOException x) {
			return null;
		} catch(InterruptedException x) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@NonNull
	private static String readFully(@NonNull InputStream is) throws IOException {
		try(InputStream in = is) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(65536);
			byte[] buffer = new byte[8192];
			int szrd;
			while(-1 != (szrd = in.read(buffer))) {
				bos.write(buffer, 0, szrd);
			}
			return bos.toString(StandardCharsets.UTF_8);
		}
	}
}

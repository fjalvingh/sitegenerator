package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resolves a file's created/last-modified date, preferring the file's git
 * history (the first and last commit that touched it) and falling back to
 * the filesystem's last-modified timestamp for files with no git history -
 * either because they are not tracked, or because they are not inside a git
 * repository at all (e.g. the checked-in testsite/ example, which is
 * gitignored in this repo).
 */
final class GitDateUtil {
	static final class DateInfo {
		@NonNull final LocalDate created;

		@NonNull final LocalDate modified;

		DateInfo(@NonNull LocalDate created, @NonNull LocalDate modified) {
			this.created = created;
			this.modified = modified;
		}
	}

	private GitDateUtil() {
	}

	@NonNull
	static DateInfo getDates(@NonNull File file) {
		DateInfo info = getGitDates(file);
		if(null != info)
			return info;
		LocalDate fsDate = getFileSystemDate(file);
		return new DateInfo(fsDate, fsDate);
	}

	/**
	 * Runs "git log --follow" for the file inside its own directory, so this
	 * works regardless of where the git repository root actually is. Returns
	 * null if the file has no git history (untracked, or not in a repo at
	 * all), letting the caller fall back to the filesystem date.
	 */
	@Nullable
	private static DateInfo getGitDates(File file) {
		File dir = file.getParentFile();
		if(null == dir || !dir.exists())
			return null;
		try {
			Process process = new ProcessBuilder("git", "log", "--follow", "--format=%aI", "--", file.getName())
				.directory(dir)
				.redirectErrorStream(false)
				.start();

			List<String> lines = new ArrayList<>();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while(null != (line = reader.readLine())) {
					if(!line.isBlank())
						lines.add(line.trim());
				}
			}
			boolean finished = process.waitFor(5, TimeUnit.SECONDS);
			if(!finished) {
				process.destroyForcibly();
				return null;
			}
			if(process.exitValue() != 0 || lines.isEmpty())
				return null;

			//-- git log lists newest commit first, oldest (creation) last
			LocalDate modified = OffsetDateTime.parse(lines.get(0)).toLocalDate();
			LocalDate created = OffsetDateTime.parse(lines.get(lines.size() - 1)).toLocalDate();
			return new DateInfo(created, modified);
		} catch(IOException | InterruptedException x) {
			return null;
		}
	}

	@NonNull
	private static LocalDate getFileSystemDate(File file) {
		try {
			return Instant.ofEpochMilli(Files.getLastModifiedTime(file.toPath()).toMillis())
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
		} catch(IOException x) {
			return LocalDate.now();
		}
	}
}

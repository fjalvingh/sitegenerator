package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.sigeto.utils.Pair;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Remembers where documents used to live, so that links to their old location -
 * both on the internet and inside the site's own sources - can be kept working
 * after a page or a whole article directory is moved.
 *
 * Moves are keyed exactly like {@link Content}'s item map: content-root relative
 * source paths using forward slashes, without a leading slash, keeping their
 * source extension ("pdp-1144/rl02/second-controller/index.md").
 *
 * Only documents are tracked. A resource - an image, a pdf - has no url a
 * redirect page could be served at, and it moves along with the document
 * directory it belongs to, so the plain "does this exist" check on its links
 * is all that is needed for it.
 *
 * The map is kept in a "redirects.tsv" file in the site root, next to content/
 * and templates/ - deliberately not inside content/, which would copy it into
 * the generated site. That file is meant to be committed: it is what keeps old
 * URLs alive once the move has scrolled out of the git history that
 * {@link GitMoveScanner} can see.
 */
public class MoveMap {
	/** The name of the map file, inside the site root. */
	public static final String FILENAME = "redirects.tsv";

	private static final String HEADER = ""
		+ "# sigeto redirect map: <old path><TAB><new path>, relative to content/.\n"
		+ "# Maintained automatically from the renames git detects, and used to\n"
		+ "# generate redirect pages at the old locations. Hand edits are kept;\n"
		+ "# commit this file.\n"
		+ "#\n"
		+ "# An optional '#moves' line says which of the renames git detects are\n"
		+ "# recorded here, and so get a redirect page keeping their old url alive:\n"
		+ "#   #moves off              none of them, while a site is being restructured\n"
		+ "#   #moves since <commit>   only the renames made after <commit>\n"
		+ "#   #moves all              every rename in the history (the default)\n"
		+ "# The moves already listed below keep working whatever it says, and stale\n"
		+ "# links in the markdown sources are repaired from every rename regardless.\n";

	/** The name of the line that decides which git renames are collected. */
	private static final String DIRECTIVE = "#moves";

	/** What to collect from git's rename history. */
	public enum Tracking {
		/** Every rename git can see: the default when there is no '#moves' line. */
		All,

		/**
		 * No redirects at all - the site is being reorganised and its old urls do
		 * not matter (yet). Stale links in the sources are still repaired.
		 */
		Off,

		/** Only the renames made after {@link MoveMap#getSinceCommit()}. */
		Since
	}

	@NonNull
	private final File m_mapFile;

	/** All known moves, old path to new path. */
	@NonNull
	private final Map<String, String> m_moveMap = new TreeMap<>();

	/** The moves that actually point at existing content, filled by {@link #resolve}. */
	@NonNull
	private final Map<String, String> m_usableMap = new TreeMap<>();

	/**
	 * Every rename git detected, whether or not '#moves' wants it recorded here.
	 * A link in the sources pointing at the old location is wrong no matter what
	 * the site decided about keeping that old url alive, so these repair links
	 * even when they generate no redirect.
	 */
	@NonNull
	private final Map<String, String> m_detectedMap = new TreeMap<>();

	/** The moves usable to repair links in the sources, filled by {@link #resolve}. */
	@NonNull
	private final Map<String, String> m_repairMap = new TreeMap<>();

	/** The file's content as it was loaded, to detect whether it needs rewriting. */
	@NonNull
	private String m_loadedText = "";

	/** Which renames to collect from git, from the '#moves' line. */
	@NonNull
	private Tracking m_tracking = Tracking.All;

	/** For {@link Tracking#Since}: the commit to start collecting renames after. */
	@Nullable
	private String m_sinceCommit;

	private int m_addedCount;

	private MoveMap(@NonNull File mapFile) {
		m_mapFile = mapFile;
	}

	/**
	 * Load the redirect map from the site root; a missing file just yields an
	 * empty map.
	 */
	@NonNull
	public static MoveMap load(@NonNull File siteRoot) throws Exception {
		File mapFile = new File(siteRoot, FILENAME);
		MoveMap map = new MoveMap(mapFile);
		if(!mapFile.exists())
			return map;

		String text = Util.readFileAsString(mapFile);
		map.m_loadedText = text;
		int lineNumber = 0;
		for(String line : text.split("\n")) {
			lineNumber++;
			String trimmed = line.trim();
			if(trimmed.isEmpty())
				continue;
			if(trimmed.startsWith("#")) {
				map.parseDirectiveIf(trimmed, mapFile, lineNumber);
				continue;
			}
			int tab = trimmed.indexOf('\t');
			if(tab < 1 || tab == trimmed.length() - 1)
				throw new MessageException(mapFile + "(" + lineNumber + "): expected '<old path><TAB><new path>' but got: " + line);
			String oldPath = trimmed.substring(0, tab).trim();
			String newPath = trimmed.substring(tab + 1).trim();
			if(oldPath.isEmpty() || newPath.isEmpty())
				throw new MessageException(mapFile + "(" + lineNumber + "): empty path in: " + line);
			map.m_moveMap.put(oldPath, newPath);
		}
		return map;
	}

	/**
	 * Handle a comment line that is really the '#moves' directive: it decides
	 * which of the renames git knows about are recorded in this map, and so get
	 * a redirect keeping their old url alive. A site that is being reorganised
	 * does not want any of them - its pages have not been anywhere yet - and can
	 * switch collecting on once it has settled down, from the commit it settled
	 * down at. It says nothing about repairing the site's own links, which is
	 * done from every rename regardless.
	 */
	private void parseDirectiveIf(@NonNull String line, @NonNull File mapFile, int lineNumber) {
		String rest = line.substring(1).trim();							// Drop the '#'
		if(!rest.toLowerCase().startsWith(DIRECTIVE.substring(1)))
			return;														// An ordinary comment
		rest = rest.substring(DIRECTIVE.length() - 1).trim();
		String where = mapFile + "(" + lineNumber + "): ";
		if(rest.equalsIgnoreCase("off")) {
			m_tracking = Tracking.Off;
		} else if(rest.equalsIgnoreCase("all") || rest.isEmpty()) {
			m_tracking = Tracking.All;
		} else if(rest.toLowerCase().startsWith("since")) {
			String commit = rest.substring("since".length()).trim();
			if(commit.isEmpty())
				throw new MessageException(where + DIRECTIVE + " since: needs a commit to start collecting renames after");
			m_tracking = Tracking.Since;
			m_sinceCommit = commit;
		} else {
			throw new MessageException(where + "unknown '" + DIRECTIVE + "' option '" + rest + "'; expected off, all or since <commit>");
		}
	}

	/**
	 * Which of the renames git knows about should end up in this map.
	 */
	@NonNull
	public Tracking getTracking() {
		return m_tracking;
	}

	/**
	 * The commit that {@link Tracking#Since} collects renames after, or null
	 * when all of them (or none) are collected.
	 */
	@Nullable
	public String getSinceCommit() {
		return m_tracking == Tracking.Since ? m_sinceCommit : null;
	}

	/**
	 * Add renames, which must be in chronological order, collapsing chained
	 * moves: when a document that was already moved moves again every old
	 * location it ever had is repointed at its newest one.
	 */
	public void mergeRenames(@NonNull List<Pair<String, String>> renameList) {
		mergeInto(m_moveMap, renameList, true);
	}

	/**
	 * Take in renames without recording them as redirects: they only serve to
	 * repair stale links in the markdown sources. Everything git detected is
	 * passed through here, whatever the '#moves' directive says.
	 */
	public void mergeDetectedRenames(@NonNull List<Pair<String, String>> renameList) {
		mergeInto(m_detectedMap, renameList, false);
	}

	private void mergeInto(@NonNull Map<String, String> map, @NonNull List<Pair<String, String>> renameList, boolean count) {
		for(Pair<String, String> rename : renameList) {
			String oldPath = rename.getFirst();
			String newPath = rename.getSecond();
			if(oldPath.equals(newPath))
				continue;
			if(!isDocument(oldPath) || !isDocument(newPath))
				continue;

			//-- Everything that pointed at the old location now points at the new one
			for(Map.Entry<String, String> entry : map.entrySet()) {
				if(entry.getValue().equals(oldPath)) {
					entry.setValue(newPath);
				}
			}

			//-- And the old location itself becomes a redirect too
			String previous = map.put(oldPath, newPath);
			if(null == previous && count) {
				m_addedCount++;
			}
		}
	}

	/**
	 * Work out which moves can actually be used: collapse any chains left by
	 * hand edits, drop moves whose old location has since been filled with new
	 * content (real content always wins from a redirect), and report moves
	 * whose target no longer exists. Unusable entries stay in the file - the
	 * record of the move is still worth keeping - they just produce no
	 * redirect.
	 *
	 * The moves that repair links in the sources are worked out the same way, but
	 * separately: they also include the renames the '#moves' directive did not
	 * want recorded.
	 */
	public void resolve(@NonNull Content content, @NonNull List<Message> errorList) {
		m_usableMap.clear();
		m_repairMap.clear();

		//-- Drop resource moves an older version of this file may still hold
		m_moveMap.entrySet().removeIf(a -> !isDocument(a.getKey()) || !isDocument(a.getValue()));
		collapse(m_moveMap);

		for(Map.Entry<String, String> entry : m_moveMap.entrySet()) {
			String oldPath = entry.getKey();
			String newPath = entry.getValue();
			if(null != content.findItem(oldPath))				// The old path is live content again
				continue;
			if(null == content.findItem(newPath)) {
				errorList.add(new Message(null, 0, MsgType.Warning, FILENAME + ": " + oldPath + " moved to " + newPath + ", which does not exist - no redirect generated"));
				continue;
			}
			m_usableMap.put(oldPath, newPath);
		}

		//-- Links are repaired from the recorded moves plus every other rename git saw
		Map<String, String> repairMap = new TreeMap<>(m_detectedMap);
		repairMap.putAll(m_moveMap);
		repairMap.entrySet().removeIf(a -> !isDocument(a.getKey()) || !isDocument(a.getValue()));
		collapse(repairMap);
		for(Map.Entry<String, String> entry : repairMap.entrySet()) {
			if(null != content.findItem(entry.getKey()))				// The old path is live content again
				continue;
			if(null == content.findItem(entry.getValue()))				// Moved on out of sight: nothing to point a link at
				continue;
			m_repairMap.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Point every old location in this map straight at the newest one, and drop
	 * the entries left pointing at themselves: a document that moved away and
	 * back again did not move at all.
	 */
	private void collapse(@NonNull Map<String, String> map) {
		for(Map.Entry<String, String> entry : map.entrySet()) {
			entry.setValue(collapseChain(map, entry.getKey(), entry.getValue()));
		}
		map.entrySet().removeIf(a -> a.getKey().equals(a.getValue()));
	}

	/**
	 * T if this path addresses a document, which is the only thing worth
	 * remembering a move for: it is the only thing with an url of its own.
	 */
	static boolean isDocument(@NonNull String path) {
		String extension = Util.getExtension(path).toLowerCase();
		return "md".equals(extension) || "mdown".equals(extension);
	}

	/**
	 * Follow a move to its final target, in case the map itself contains a
	 * chain (a -&gt; b, b -&gt; c). Stops on a cycle.
	 */
	@NonNull
	private String collapseChain(@NonNull Map<String, String> map, @NonNull String oldPath, @NonNull String newPath) {
		Set<String> seen = new HashSet<>();
		seen.add(oldPath);
		String path = newPath;
		while(seen.add(path)) {
			String next = map.get(path);
			if(null == next)
				break;
			path = next;
		}
		return path;
	}

	/**
	 * The current location of a document that used to be at the given content
	 * relative path, for repairing a link that still points at it, or null if
	 * that path was never moved (or its target no longer exists). This knows
	 * about every rename git detected, not just the ones recorded as redirects.
	 */
	@Nullable
	public String getTarget(@NonNull String oldPath) {
		return m_repairMap.get(oldPath);
	}

	/**
	 * All usable moves, old path to new path, sorted by old path.
	 */
	@NonNull
	public Map<String, String> getUsableMoves() {
		return m_usableMap;
	}

	/**
	 * Write the map back out if it changed, so that moves detected from git
	 * history are remembered even after that history is rewritten or the
	 * content is copied into another repository.
	 */
	public void saveIfChanged() throws Exception {
		if(m_moveMap.isEmpty() && !m_mapFile.exists())					// Nothing ever moved: do not litter the site root
			return;
		StringBuilder sb = new StringBuilder(HEADER.length() + m_moveMap.size() * 80);
		sb.append(HEADER);
		switch(m_tracking) {											// Keep the directive: it is the site's decision, not ours
			default:
				break;

			case Off:
				sb.append(DIRECTIVE).append(" off\n");
				break;

			case Since:
				sb.append(DIRECTIVE).append(" since ").append(m_sinceCommit).append('\n');
				break;
		}
		for(Map.Entry<String, String> entry : m_moveMap.entrySet()) {
			sb.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
		}
		String text = sb.toString();
		if(text.equals(m_loadedText))
			return;
		Util.writeFileFromString(m_mapFile, text, StandardCharsets.UTF_8);
		m_loadedText = text;
		if(m_addedCount > 0) {
			System.out.println("Redirect map updated: " + m_addedCount + " new move(s) in " + FILENAME + " - please commit it");
		} else {
			System.out.println("Redirect map rewritten: " + FILENAME + " - please commit it");
		}
	}
}

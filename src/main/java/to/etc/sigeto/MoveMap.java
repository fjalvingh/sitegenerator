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
		+ "# commit this file.\n";

	@NonNull
	private final File m_mapFile;

	/** All known moves, old path to new path. */
	@NonNull
	private final Map<String, String> m_moveMap = new TreeMap<>();

	/** The moves that actually point at existing content, filled by {@link #resolve}. */
	@NonNull
	private final Map<String, String> m_usableMap = new TreeMap<>();

	/** The file's content as it was loaded, to detect whether it needs rewriting. */
	@NonNull
	private String m_loadedText = "";

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
			if(trimmed.isEmpty() || trimmed.startsWith("#"))
				continue;
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
	 * Add renames, which must be in chronological order, collapsing chained
	 * moves: when a document that was already moved moves again every old
	 * location it ever had is repointed at its newest one.
	 */
	public void mergeRenames(@NonNull List<Pair<String, String>> renameList) {
		for(Pair<String, String> rename : renameList) {
			String oldPath = rename.getFirst();
			String newPath = rename.getSecond();
			if(oldPath.equals(newPath))
				continue;
			if(!isDocument(oldPath) || !isDocument(newPath))
				continue;

			//-- Everything that pointed at the old location now points at the new one
			for(Map.Entry<String, String> entry : m_moveMap.entrySet()) {
				if(entry.getValue().equals(oldPath)) {
					entry.setValue(newPath);
				}
			}

			//-- And the old location itself becomes a redirect too
			String previous = m_moveMap.put(oldPath, newPath);
			if(null == previous) {
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
	 */
	public void resolve(@NonNull Content content, @NonNull List<Message> errorList) {
		m_usableMap.clear();

		//-- Drop resource moves an older version of this file may still hold
		m_moveMap.entrySet().removeIf(a -> !isDocument(a.getKey()) || !isDocument(a.getValue()));


		//-- Point every old location straight at the newest one
		for(Map.Entry<String, String> entry : m_moveMap.entrySet()) {
			entry.setValue(collapseChain(entry.getKey(), entry.getValue()));
		}

		//-- A document that moved away and back again did not move at all
		m_moveMap.entrySet().removeIf(a -> a.getKey().equals(a.getValue()));

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
	private String collapseChain(@NonNull String oldPath, @NonNull String newPath) {
		Set<String> seen = new HashSet<>();
		seen.add(oldPath);
		String path = newPath;
		while(seen.add(path)) {
			String next = m_moveMap.get(path);
			if(null == next)
				break;
			path = next;
		}
		return path;
	}

	/**
	 * The current location of a document that used to be at the given content
	 * relative path, or null if that path was never moved (or its target no
	 * longer exists).
	 */
	@Nullable
	public String getTarget(@NonNull String oldPath) {
		return m_usableMap.get(oldPath);
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

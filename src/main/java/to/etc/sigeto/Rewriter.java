package to.etc.sigeto;

import to.etc.sigeto.utils.IndentWriter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * Rewrite the site structure from the confluence export
 * format to proper side format. It moves each article into
 * its own directory, and moves resources used by an article
 * next to the article.
 */
public class Rewriter {
	private final Content m_content;

	private final MarkdownChecker m_mc;

	private final File m_output;

	private final Map<ContentItem, StringBuilder> m_textMap = new HashMap<ContentItem, StringBuilder>();

	/** New locations for all items */
	private final Map<ContentItem, File> m_moveMap = new HashMap<>();

	private final static class XrefItem {
		/** The item that uses the reference item */
		private final ContentItem m_item;

		/** The names that are used in the text of this item */
		private final List<String> m_relativeNameList = new ArrayList<>();

		public XrefItem(ContentItem item, Set<String> paths) {
			m_item = item;
			m_relativeNameList.addAll(paths);
		}
	}

	private final Map<ContentItem, List<XrefItem>> m_xrefMap = new HashMap<ContentItem, List<XrefItem>>();

	public Rewriter(Content content, MarkdownChecker mc, File output) {
		m_content = content;
		m_mc = mc;
		m_output = output;
	}

	public static void rewrite(Content content, MarkdownChecker mc, File output) throws Exception {
		new Rewriter(content, mc, output).run();
	}

	private void run() throws Exception {
		ContentLevel rootLevel = m_content.getPageRootLevel();
		if(null == rootLevel) {
			throw new IllegalStateException("root level is null");
		}

		//-- Load all markdown files as text in memory, and build an xref map
		for(ContentItem item : m_content.getItemList()) {
			if(item.getFileType() == ContentFileType.Markdown) {
				StringBuilder sb = new StringBuilder();
				sb.append(Util.readFileAsString(item.getFile()));
				m_textMap.put(item, sb);
			}

			for(Entry<ContentItem, Set<String>> usedItem : item.getUsedItemList().entrySet()) {
				List<XrefItem> xrefList = m_xrefMap.computeIfAbsent(usedItem.getKey(), contentItem -> new ArrayList<>());
				xrefList.add(new XrefItem(item, usedItem.getValue()));
			}

			File newLocation = new File(m_output, item.getRelativePath());
			m_moveMap.put(item, newLocation);
		}

		//-- Start writing back the data.
		Util.dirEmpty(m_output);
		for(ContentItem contentItem : m_content.getItemList()) {
			File file = m_moveMap.get(contentItem);
			if(null == file) {
				throw new IllegalStateException("file is null for " + contentItem);
			}
			file.getParentFile().mkdirs();
			if(contentItem.getFileType() == ContentFileType.Markdown) {
				StringBuilder text = m_textMap.get(contentItem);
				Util.writeFileFromString(file, text.toString(), StandardCharsets.UTF_8);
			} else {
				Util.copyFile(file, contentItem.getFile());
			}
		}

		//-- Start handling all levels: find an Index page for each level.
		fixLevelIndex(rootLevel);





		//IndentWriter w = new IndentWriter(new OutputStreamWriter(System.out));
		//renderInfo(w, rootLevel);
	}

	private void fixLevelIndex(ContentLevel level) {
		if(level.getParentLevel() != null) {
			findIndexOrRename(level);
		}
		for(ContentLevel sub : level.getSubLevelList()) {
			fixLevelIndex(sub);
		}
	}

	private void findIndexOrRename(ContentLevel level) {
		ContentItem rootItem = level.getRootItem();
		if(null != rootItem || !level.hasMarkdown()) {
			return;
		}

		//-- No root item. Does an item exist with the same name as a .md file upwards?
		ContentItem up = Objects.requireNonNull(level.getParentLevel()).findItemByName(level.getName() + ".md");
		if(up == null)
			throw new IllegalStateException("Cannot find a parent document for sublevel " + level.getRelativePath());

		//-- We want to move this downwards. Create a new relative name
		String newRelPath = level.getRelativePath() + "/index.md";

		System.out.println("Renaming " + up.getRelativePath() + " to " + newRelPath);
		moveFile(up, level, "index.md");
	}

	/**
	 * Move a content item to some new location, and handle all chores
	 * related to that.
	 * When we move a document we need to do the following:
	 * - Move the document to a new location (by setting its output location)
	 * - Move all non-md subitems that are uniquely used by the document to the
	 *   new location too (i.e. all attachments belonging to the document)
	 * - We now have a list of old -> new items. Inside the moved document:
	 * -- Rename all references. If they are unique to the document they do not
	 *    really move because the items moved with them, but otherwise calculate
	 *    a new relative path and use that.
	 * -- All documents that use the document: fix the reference.
	 */
	private void moveFile(ContentItem item, ContentLevel targetLevel, String newName) {
		String oldPath = item.getRelativePath();
		item.moveTo(targetLevel, newName);					// Mark the file as moved
		String newPath = item.getRelativePath();
		System.out.println("Moved " + oldPath + " to " + newPath);

		File targetLocation = new File(m_output, item.getRelativePath());
		m_moveMap.put(item, targetLocation);						// Cause it to actually move

		//-- Register moves for all content items used by this page only
		for(Entry<ContentItem, Set<String>> usedItem : item.getUsedItemList().entrySet()) {
			ContentItem used = usedItem.getKey();
			if(used.getFileType() != ContentFileType.Markdown) {
				List<XrefItem> xrefItems = m_xrefMap.get(used);
				if(xrefItems == null || xrefItems.size() != 1) {
					if(!used.isInside(targetLevel)) {
						//-- We'll have to move these, as well; this is just a simple file move though
						oldPath = used.getRelativePath();
						used.moveTo(targetLevel, used.getName());
						newPath = used.getRelativePath();
						System.out.println("Moved " + oldPath + " to " + newPath);
						m_moveMap.put(used, new File(m_output, used.getRelativePath()));
					}
				}
			}
		}







	}

	/**
	 */
	private void renameReferences(ContentItem item, String newRootRelativeLocation) {









	}


	private void renderInfo(IndentWriter w, ContentLevel level) throws Exception {
		w.println("Level " + level.getRelativePath());
		w.inc();
		for(ContentItem item : level.getSubItems()) {
			w.println("Item " + item.getRelativePath());
			w.inc();
			for(Map.Entry<ContentItem, Set<String>> used : item.getUsedItemList().entrySet()) {
				if(used.getValue().size() == 1) {
					w.println("Uses " + used.getKey().getRelativePath() + " via " + used.getValue().iterator().next());
				} else {
					w.println("Uses " + used.getKey().getRelativePath());
					w.inc();
					for(String s : used.getValue()) {
						w.println("via " + s);
					}

					w.dec();
				}
			}
			w.dec();
		}
		w.dec();
		w.println("-- sublevels");
		w.inc();
		for(ContentLevel subLevel : level.getSubLevelList()) {
			renderInfo(w, subLevel);
		}
		w.dec();
	}
}

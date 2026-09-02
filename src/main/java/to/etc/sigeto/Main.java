package to.etc.sigeto;

import gg.jte.CodeResolver;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.MapOptionHandler;
import to.etc.sigeto.utils.Pair;
import to.etc.sigeto.variables.VariableFile;
import to.etc.sigeto.variables.Variables;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
	@Option(name = "-i", aliases = {"-input"}, usage = "The directory containing the site's source files", required = true)
	private String m_inputRoot;

	@Option(name = "-o", aliases = {"-output"}, usage = "The output directory, default is _output in the site root")
	private String m_outputRoot;

	@Option(name = "-D", metaVar = "name=value", handler = MapOptionHandler.class,
		usage = "Define a variable the documentation can use as ${name}, overriding " + VariableFile.FILENAME + "; may be repeated")
	private Map<String, String> m_variableMap = new LinkedHashMap<>();

	/** The variable that stands for the base url the "!demo(path)" tags resolve against. */
	private static final String DEMO_VARIABLE = "demo";

	static public void main(String[] args) {
		try {
			new Main().run(args);
		} catch(Exception x) {
			log("Fatal error: " + x);
			x.printStackTrace();
		}
	}

	private void run(String[] args) throws Exception {
		CmdLineParser p = new CmdLineParser(this);
		try {
			//-- Decode the tasks's arguments
			p.parseArgument(splitDefines(args));
		} catch(CmdLineException x) {
			System.err.println("Invalid arguments: " + x.getMessage());
			p.printUsage(System.err);
			System.exit(10);
		}

		try {
			File sourceRoot = new File(m_inputRoot);
			if(!sourceRoot.exists() || !sourceRoot.isDirectory()) {
				throw new MessageException("Source root does not exist: " + m_inputRoot);
			}
			File outputRoot = m_outputRoot == null ? new File(sourceRoot, "_output"): new File(m_outputRoot);
			if(!outputRoot.exists()) {
				outputRoot.mkdirs();
			}
			if(!outputRoot.exists()) {
				throw new MessageException("Output root does not exist and cannot be created at " + outputRoot);
			}
			File templateRoot = new File(m_inputRoot, "templates");
			if(!templateRoot.exists() || !templateRoot.isDirectory()) {
				throw new MessageException("Template root does not exist: " + templateRoot);
			}

			//-- Find the content root
			File contentRoot = new File(m_inputRoot, "content");
			if(!contentRoot.exists() || !contentRoot.isDirectory()) {
				throw new MessageException("Content root does not exist: " + contentRoot);
			}

			Content content = Content.create(contentRoot);
			if(content.getMarkDownItemCount() == 0) {
				throw new MessageException("No markdown source files found at " + sourceRoot);
			}

			List<Message> errorList = new ArrayList<>();

			//-- Find out which documents moved, so old urls and stale links can be handled
			MoveMap moveMap = MoveMap.load(sourceRoot);

			//-- Every rename repairs the links in the sources; '#moves' only decides which are kept as redirects
			List<Pair<String, String>> renameList = GitMoveScanner.scanRenames(contentRoot, null);
			moveMap.mergeDetectedRenames(renameList);
			switch(moveMap.getTracking()) {
				case Off:
					reportMoveTrackingOff(contentRoot);
					break;

				case Since:									// A smaller set than the one scanned above
					moveMap.mergeRenames(GitMoveScanner.scanRenames(contentRoot, moveMap.getSinceCommit()));
					break;

				default:
					moveMap.mergeRenames(renameList);
					break;
			}
			moveMap.resolve(content, errorList);
			moveMap.saveIfChanged();

			//-- Scan all markdown files, and check them
			List<ContentItem> markdownList = content.getItemList().stream()
				.filter(a -> a.getFileType() == ContentFileType.Markdown)
				.collect(Collectors.toList());
			int mdFiles = 0;
			int blogFiles = 0;
			Map<String, String> variableMap = variables(sourceRoot);
			String includeBase = demoBase(variableMap);
			MarkdownChecker mc = new MarkdownChecker(content, outputRoot, moveMap, includeBase, variableMap::get);
			for(ContentItem item : markdownList) {
				mc.scanContent(errorList, item);
				if(item.getType() == ContentType.Page) {
					mdFiles++;
				} else if(item.getType() == ContentType.Blog) {
					blogFiles++;
					System.out.println("blog>> " + item.getRelativePath());
				}
			}
			System.out.println("Found " + mdFiles + " pages and " + blogFiles + " blog items");

			//-- Links into a place inside a page can only be checked once every page has been scanned
			mc.checkAnchors(errorList);

			//-- Repair the links to moved documents in the sources themselves
			List<LinkFix> linkFixList = mc.getLinkFixList();
			if(!linkFixList.isEmpty()) {
				SourceLinkFixer.apply(linkFixList, errorList);
			}

			if(!errorList.isEmpty()) {
				for(Message message : errorList) {
					System.err.println(message);
				}

				//-- Warnings are worth reporting but should not stop the build
				if(errorList.stream().anyMatch(a -> a.getType() == MsgType.Error)) {
					System.exit(9);
				}
			}
			content.complete();

			//-- Now render
			CodeResolver codeResolver = new DirectoryCodeResolver(Path.of(templateRoot.toString())); // This is the directory where your .jte files are located.

			//-- jte compiles the templates to class files; keep those out of the site's own directories
			Path jteClassPath = Files.createTempDirectory("sigeto-jte");
			try {
				TemplateEngine templateEngine = TemplateEngine.create(codeResolver, jteClassPath, gg.jte.ContentType.Html);
				Util.dirEmpty(outputRoot);
				for(ContentItem item : content.getItemList()) {
					renderItem(outputRoot, templateEngine, mc, item, content);
				}

				//-- Render the sitewide global timeline copy of every story-nested blog entry
				for(ContentLevel blog : content.getAllBlogEntries()) {
					if(!blog.isGlobalBlogRoot()) {
						renderGlobalBlogMirror(outputRoot, templateEngine, mc, blog, content);
					}
				}

				//-- Copy theme data
				copyTemplateAssets(outputRoot, templateRoot);

				//-- The menu as data, for sites that build their menu in the browser
				MenuJsonWriter.write(outputRoot, content.getMenu());

				//-- And keep the urls of everything that moved working
				RedirectWriter.write(outputRoot, templateRoot, templateEngine, content, moveMap);
			} finally {
				Util.deleteDir(jteClassPath.toFile());
			}


		} catch(MessageException x) {
			System.err.println("Error: " + x.getMessage());
			System.exit(10);
		} catch(Exception x) {
			x.printStackTrace();
			System.exit(10);
		}
	}

	private void copyTemplateAssets(File outputRoot, File templateRoot) throws Exception {
		File[] files = templateRoot.listFiles();
		if(null == files)
			return;
		for(File file : files) {
			if(file.isDirectory()) {
				copyTemplateAssets(new File(outputRoot, file.getName()), file);
			} else {
				if(!file.getName().toLowerCase().endsWith(".jte")) {
					outputRoot.mkdirs();
					Util.copyFile(new File(outputRoot, file.getName()), file);
				}
			}
		}
	}

	private void renderItem(File outputRoot, TemplateEngine templateEngine, MarkdownChecker mc, ContentItem item, Content content) throws Exception {
		if(item.getFileType() == ContentFileType.Markdown) {
			renderMarkdown(outputRoot, templateEngine, mc, item, content);
		} else {
			String relativePath = item.getRelativePath();
			File out = new File(outputRoot, relativePath);
			File parentFile = out.getParentFile();
			if(null != parentFile) {
				parentFile.mkdirs();
			}
			Util.copyFile(out, item.getFile());
		}
	}

	/**
	 * Say that no redirects are being recorded, and how to start recording them
	 * from this point on once the site has settled down. Printed every build
	 * because forgetting that it is off is exactly how old urls get lost.
	 */
	private static void reportMoveTrackingOff(File contentRoot) {
		String head = GitMoveScanner.currentCommit(contentRoot);
		String hint = head == null
			? ""
			: " To start recording them from here on, make that line: #moves since " + head;
		System.out.println("Move tracking is off ('#moves off' in " + MoveMap.FILENAME + "): renames repair the links in the sources but get no redirect page." + hint);
	}

	/**
	 * What the "${name}" variables in the documentation stand for: the site's
	 * own definitions in {@link VariableFile}, with whatever the command line
	 * defined with -D on top of them - so that the same site can be built
	 * against another installation without editing the file it commits.
	 */
	@NonNull
	private Map<String, String> variables(@NonNull File siteRoot) throws Exception {
		Map<String, String> map = VariableFile.load(siteRoot);
		for(Map.Entry<String, String> entry : m_variableMap.entrySet()) {
			String name = entry.getKey();
			if(!Variables.isValidName(name))
				throw new MessageException("-D" + name + "=...: a variable name can only contain letters, digits, '.', '-' and '_'");
			map.put(name, entry.getValue());
		}
		return map;
	}

	/**
	 * The base url the "!demo(path)" tags resolve against: the ${demo}
	 * variable, checked as far as it can be - whether it actually serves
	 * anything is only known at the moment someone looks at the page. A site
	 * using no !demo() tags needs no ${demo}, so a missing one is not an error
	 * here but at the tag that needs it.
	 */
	@Nullable
	private static String demoBase(@NonNull Map<String, String> variableMap) {
		String base = variableMap.get(DEMO_VARIABLE);
		if(null == base)
			return null;
		base = base.trim();
		if(base.isEmpty())
			throw new MessageException("${" + DEMO_VARIABLE + "}: the base url of the application is empty");
		if(!base.toLowerCase().startsWith("http://") && !base.toLowerCase().startsWith("https://"))
			throw new MessageException("${" + DEMO_VARIABLE + "} " + base + ": the base url of the application must start with http:// or https://");
		variableMap.put(DEMO_VARIABLE, base);						// The tags and ${demo} must mean the same thing
		return base;
	}

	/**
	 * Accept "-Dname=value" as well as the "-D name=value" that args4j knows,
	 * because the glued form is the one everybody types.
	 */
	private static String[] splitDefines(String[] args) {
		List<String> list = new ArrayList<>(args.length + 2);
		for(String arg : args) {
			if(arg.startsWith("-D") && arg.length() > 2) {
				list.add("-D");
				list.add(arg.substring(2));
			} else {
				list.add(arg);
			}
		}
		return list.toArray(new String[list.size()]);
	}

	private static void renderMarkdown(File outputRoot, TemplateEngine templateEngine, MarkdownChecker mc, ContentItem item, Content content) throws Exception {
		String outputDir = item.getDirectoryPath();
		String render = mc.renderContent(item, outputDir);
		String newPath = item.getRelativeTargetPath();

		BlogNav nav = BlogNav.NONE;
		if(item.getType() == ContentType.Blog) {
			ContentLevel level = item.getLevel();
			nav = level.isGlobalBlogRoot()
				? BlogNav.global(content, level, outputDir)
				: BlogNav.local(level, outputDir);
		}

		writePage(outputRoot, templateEngine, mc, content, item, render, newPath, nav);
	}

	/**
	 * Render a story-nested blog entry a second time, into the sitewide global
	 * blog timeline namespace, with prev/next navigating the whole site's
	 * chronological blog list rather than just this entry's story.
	 */
	private static void renderGlobalBlogMirror(File outputRoot, TemplateEngine templateEngine, MarkdownChecker mc, ContentLevel blog, Content content) throws Exception {
		ContentItem item = blog.getRootItem();
		if(null == item)
			return;
		String outputDir = "blog-timeline/" + item.getDirectoryPath();
		String render = mc.renderContent(item, outputDir);
		String newPath = "blog-timeline/" + item.getRelativeTargetPath();

		BlogNav nav = BlogNav.global(content, blog, outputDir);
		writePage(outputRoot, templateEngine, mc, content, item, render, newPath, nav);
	}

	private static void writePage(File outputRoot, TemplateEngine templateEngine, MarkdownChecker mc, Content content, ContentItem item, String render, String newPath, BlogNav nav) throws Exception {
		File out = new File(outputRoot, newPath);
		File parentFile = out.getParentFile();
		if(null != parentFile) {
			parentFile.mkdirs();
		}

		TemplateOutput output = new StringOutput(65536);
		PageModel pm = new PageModel(content, render, mc, item, nav.previousHref, nav.previousTitle, nav.nextHref, nav.nextTitle);
		templateEngine.render("base.jte", pm, output);

		Util.writeFileFromString(out, output.toString(), StandardCharsets.UTF_8);
	}

	/**
	 * Resolved, already-relative prev/next navigation for a blog entry render.
	 */
	private static final class BlogNav {
		static final BlogNav NONE = new BlogNav(null, null, null, null);

		@Nullable final String previousHref;

		@Nullable final String previousTitle;

		@Nullable final String nextHref;

		@Nullable final String nextTitle;

		BlogNav(@Nullable String previousHref, @Nullable String previousTitle, @Nullable String nextHref, @Nullable String nextTitle) {
			this.previousHref = previousHref;
			this.previousTitle = previousTitle;
			this.nextHref = nextHref;
			this.nextTitle = nextTitle;
		}

		/**
		 * Nav scoped to the blog entries directly nested under the same parent (story).
		 */
		static BlogNav local(ContentLevel level, String outputDir) {
			ContentLevel prev = Content.getPreviousLocalBlog(level);
			ContentLevel next = Content.getNextLocalBlog(level);
			return of(outputDir, prev, next, l -> {
				ContentItem root = l.getRootItem();
				return null == root ? null : root.getRelativeTargetPath();
			});
		}

		/**
		 * Nav scoped to the sitewide chronological list of all blog entries.
		 */
		static BlogNav global(Content content, ContentLevel level, String outputDir) {
			ContentLevel prev = content.getPreviousGlobalBlog(level);
			ContentLevel next = content.getNextGlobalBlog(level);
			return of(outputDir, prev, next, ContentLevel::getGlobalOutputPath);
		}

		private static BlogNav of(String outputDir, @Nullable ContentLevel prev, @Nullable ContentLevel next, java.util.function.Function<ContentLevel, String> targetPath) {
			String prevHref = null;
			String prevTitle = null;
			if(null != prev) {
				String path = targetPath.apply(prev);
				ContentItem root = prev.getRootItem();
				if(null != path && null != root) {
					prevHref = Util.relativeHref(outputDir, path);
					prevTitle = root.getPageTitle();
				}
			}
			String nextHref = null;
			String nextTitle = null;
			if(null != next) {
				String path = targetPath.apply(next);
				ContentItem root = next.getRootItem();
				if(null != path && null != root) {
					nextHref = Util.relativeHref(outputDir, path);
					nextTitle = root.getPageTitle();
				}
			}
			return new BlogNav(prevHref, prevTitle, nextHref, nextTitle);
		}
	}

	static private void log(String s) {
		System.out.println(s);
	}
}

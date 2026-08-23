package to.etc.sigeto;

import gg.jte.CodeResolver;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.eclipse.jdt.annotation.Nullable;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
	@Option(name = "-i", aliases = {"-input"}, usage = "The directory containing the site's source files", required = true)
	private String m_inputRoot;

	@Option(name = "-o", aliases = {"-output"}, usage = "The output directory, default is _output in the site root")
	private String m_outputRoot;

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
			p.parseArgument(args);
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
			moveMap.mergeRenames(GitMoveScanner.scanRenames(contentRoot));
			moveMap.resolve(content, errorList);
			moveMap.saveIfChanged();

			//-- Scan all markdown files, and check them
			List<ContentItem> markdownList = content.getItemList().stream()
				.filter(a -> a.getFileType() == ContentFileType.Markdown)
				.collect(Collectors.toList());
			int mdFiles = 0;
			int blogFiles = 0;
			MarkdownChecker mc = new MarkdownChecker(content, moveMap);
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

package to.etc.sigeto;

import gg.jte.CodeResolver;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
				throw new MessageException("Output root does not exist and cannot be created at " + m_outputRoot);
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

			//-- Scan all markdown files, and check them
			List<ContentItem> blogItemList = new ArrayList<>();
			List<ContentItem> markdownList = content.getItemList().stream()
				.filter(a -> a.getFileType() == ContentFileType.Markdown)
				.collect(Collectors.toList());
			int mdFiles = 0;
			int blogFiles = 0;
			MarkdownChecker mc = new MarkdownChecker(content);
			List<Message> errorList = new ArrayList<>();
			for(ContentItem item : markdownList) {
				mc.scanContent(errorList, item);
				if(item.getType() == ContentType.Page) {
					mdFiles++;
				} else if(item.getType() == ContentType.Blog) {
					blogFiles++;
					blogItemList.add(item);
				}
			}
			System.out.println("Found " + mdFiles + " pages and " + blogFiles + " blog items");

			if(!errorList.isEmpty()) {
				for(Message message : errorList) {
					System.err.println(message);
				}
				System.exit(9);
			}
			content.complete();

			//-- Now render
			CodeResolver codeResolver = new DirectoryCodeResolver(Path.of(templateRoot.toString())); // This is the directory where your .jte files are located.
			TemplateEngine templateEngine = TemplateEngine.create(codeResolver, gg.jte.ContentType.Html);
			Util.dirEmpty(outputRoot);
			for(ContentItem item : content.getItemList()) {
				renderItem(outputRoot, templateEngine, mc, item, content);
			}

			//-- Copy theme data
			copyTemplateAssets(outputRoot, templateRoot);


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
		String render = mc.renderContent(item);
		String relativePath = item.getRelativePath();
		int pos = relativePath.lastIndexOf(".");
		if(pos == -1)
			throw new IllegalStateException("?? No .md extension");
		String newPath = relativePath.substring(0, pos) + ".html";
		File out = new File(outputRoot, newPath);
		File parentFile = out.getParentFile();
		if(null != parentFile) {
			parentFile.mkdirs();
		}

		TemplateOutput output = new StringOutput(65536);
		PageModel pm = new PageModel(content, render, mc, item);
		templateEngine.render("base.jte", pm, output);

		Util.writeFileFromString(out, output.toString(), StandardCharsets.UTF_8);
	}

	static private void log(String s) {
		System.out.println(s);
	}
}

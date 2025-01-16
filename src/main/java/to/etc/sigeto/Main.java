package to.etc.sigeto;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
			List<ContentItem> markdownList = content.getItemList().stream()
				.filter(a -> a.getType() == ContentType.Markdown)
				.collect(Collectors.toList());
			MarkdownChecker mc = new MarkdownChecker(content);
			List<Message> errorList = new ArrayList<>();
			for(ContentItem item : markdownList) {
				mc.scanContent(errorList, item);
			}
			if(!errorList.isEmpty()) {
				for(Message message : errorList) {
					System.err.println(message);
				}
				System.exit(9);
			}

			//-- Now render
			for(ContentItem item : markdownList) {
				renderItem(outputRoot, mc, item);
			}




		} catch(MessageException x) {
			System.err.println("Error: " + x.getMessage());
			System.exit(10);
		} catch(Exception x) {
			x.printStackTrace();
			System.exit(10);
		}
	}

	private void renderItem(File outputRoot, MarkdownChecker mc, ContentItem item) throws Exception {
		if(item.getType() == ContentType.Markdown) {
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
			Util.writeFileFromString(out, render, StandardCharsets.UTF_8);
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
	 * Scan all content files in the content root, and define relative paths to all of them.
	 */
	private void scanContent(String inputRoot) {




	}

	static private void log(String s) {
		System.out.println(s);
	}
}

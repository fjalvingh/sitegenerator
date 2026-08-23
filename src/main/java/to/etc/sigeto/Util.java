package to.etc.sigeto;

import org.eclipse.jdt.annotation.NonNull;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Util {
	static public final long KB = 1024L;

	static public final long MB = 1024L * KB;

	static public final long GB = 1024L * MB;

	/**
	 * Read a file's contents in a string, decoded as UTF-8.
	 */
	static public String readFileAsString(final File f) throws Exception {
		StringBuilder sb = new StringBuilder((int) f.length() + 20);
		readFileAsString(sb, f);
		return sb.toString();
	}

	static public void readFileAsString(final Appendable o, final File f) throws Exception {
		try(LineNumberReader lr = new LineNumberReader(Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8))) {
			String line;
			while(null != (line = lr.readLine())) {
				o.append(line);
				o.append("\n");
			}
		}
	}

	static public void writeFileFromString(final File f, final String v, Charset enc) throws Exception {
		try(OutputStream os = new FileOutputStream(f)) {
			writeFileFromString(os, v, enc);
		}
	}

	static public void writeFileFromString(final OutputStream os, final String v, Charset enc) throws Exception {
		try(Writer w = new OutputStreamWriter(os, enc == null ? StandardCharsets.UTF_8 : enc)) {
			w.write(v);
		}
	}

	/**
	 * Copies a file of max. 1GB.
	 */
	static public void copyFile(@NonNull File destf, @NonNull File srcf) throws IOException {
		copyFile(destf, srcf, 1 * GB);
	}

	static public void copyFile(@NonNull File destf, @NonNull File srcf, long maxSize) throws IOException {
		try {
			try(InputStream is = new FileInputStream(srcf); OutputStream os = new FileOutputStream(destf)) {
				copyFile(os, is, maxSize);
			}
			ignore(destf.setLastModified(srcf.lastModified()));
		} catch(IOException x) {
			delete(destf);                                    // Clean up partial/truncated output
			throw x;
		}
	}

	/**
	 * Copies the inputstream to the output stream, limited to 1GB of data(!).
	 */
	static public void copyFile(@NonNull OutputStream os, @NonNull InputStream is) throws IOException {
		copyFile(os, is, 1 * GB);
	}

	static public void copyFile(@NonNull OutputStream os, @NonNull InputStream is, long maxSize) throws IOException {
		byte[] buf = new byte[8192];
		int sz;
		long size = 0L;
		while(0 < (sz = is.read(buf))) {
			size += sz;
			if(size > maxSize)
				throw new IOException("Copied data exceeds the configured maximum (" + maxSize + " bytes)");
			os.write(buf, 0, sz);
		}
	}

	/**
	 * Used to prevent idiotic errors from Sonar for file.delete.
	 */
	public static void ignore(boolean delete) {
		//-- And we need a nested comment too 8-(

	}

	/**
	 * Build an attribute map for HtmlWriter.tag() from name/value pairs, which
	 * keeps the order the attributes are given in. Map.of cannot be used for
	 * this: its iteration order is randomized per JVM run, which makes the
	 * generated html differ between builds of the very same site.
	 */
	@NonNull
	public static Map<String, String> attributes(String... nameValuePairs) {
		if((nameValuePairs.length % 2) != 0)
			throw new IllegalArgumentException("Expected name/value pairs but got " + nameValuePairs.length + " values");
		Map<String, String> map = new LinkedHashMap<>();
		for(int i = 0; i < nameValuePairs.length; i += 2) {
			map.put(nameValuePairs[i], nameValuePairs[i + 1]);
		}
		return map;
	}

	public static String getExtension(String name) {
		int pos = name.lastIndexOf('/');
		if(pos != -1) {
			name = name.substring(pos + 1);
		} else {
			pos = name.lastIndexOf('\\');
			if(pos != -1) {
				name = name.substring(pos + 1);
			}
		}

		pos = name.lastIndexOf(".");
		if(pos == -1) {
			return "";
		}
		return name.substring(pos + 1);
	}

	public static String getFilenameSansExtension(String name) {
		int slash = name.lastIndexOf('/');
		if(slash == -1) {
			slash = name.lastIndexOf('\\');
		}
		int pos = name.lastIndexOf(".");
		if(pos < slash)
			return name;
		return name.substring(0, pos);
	}

	/**
	 * The number of "../" segments needed to climb from the given output
	 * directory (relative to the site root, empty string for the root itself)
	 * back to the site root.
	 */
	public static String depthPrefix(String outputDir) {
		if(outputDir == null || outputDir.isEmpty())
			return "";
		int depth = 1;
		for(int i = 0; i < outputDir.length(); i++) {
			if(outputDir.charAt(i) == '/') {
				depth++;
			}
		}
		return "../".repeat(depth);
	}

	/**
	 * Compute a relative href from the given output directory (relative to the
	 * site root) to the given target path (also relative to the site root), by
	 * climbing back to the root and then descending to the target. This is not
	 * necessarily the shortest possible relative path, but it is always correct
	 * regardless of how deeply nested the current output directory is.
	 */
	public static String relativeHref(String outputDir, String targetPath) {
		return depthPrefix(outputDir) + targetPath;
	}

	public static Dimension getImageDimension(File imgFile) throws IOException {
		int pos = imgFile.getName().lastIndexOf(".");
		if(pos == -1)
			throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
		String suffix = imgFile.getName().substring(pos + 1);
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		while(iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				try(ImageInputStream stream = new FileImageInputStream(imgFile)) {
					reader.setInput(stream);
					int width = reader.getWidth(reader.getMinIndex());
					int height = reader.getHeight(reader.getMinIndex());
					return new Dimension(width, height);
				}
			} catch(IOException e) {
				System.out.println("Error reading image: " + imgFile.getAbsolutePath() + ": " + e.getMessage());
			} finally {
				reader.dispose();
			}
		}
		throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}

	/**
	 * Delete the directory <i>and</i> all it's contents.
	 */
	static public void deleteDir(@NonNull File f) {
		dirEmpty(f);
		delete(f);
	}

	/**
	 * Deletes the file or (empty) directory, and reports an error in the log if that fails.
	 */
	public static void delete(File file) {
		try {
			Files.delete(file.toPath());
		} catch(Exception x) {
			//-- ignore
		}
	}

	/**
	 * Deletes all files in the directory. It skips errors and tries to delete
	 * as much as possible. If elogb is not null then all errors are written
	 * there.
	 */
	static public boolean dirEmpty(@NonNull File dirf) {
		boolean hase = false;

		File[] ar = dirf.listFiles();
		if(ar == null)
			return true;

		for(File file : ar) {
			String name = file.getName();
			if(!name.equals(".") && !name.equals("..")) {
				if(file.isDirectory() && !dirEmpty(file)) {
					hase = true;
					continue;                              // Do not attempt to delete a non-empty directory
				}
				if(!file.delete()) {
					hase = true;
				}
			}
		}

		return !hase;
	}
}

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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Util {
	static public final long KB = 1024L;

	static public final long MB = 1024L * KB;

	static public final long GB = 1024L * MB;

	/**
	 * Read a file's contents in a string using the default encoding of the platform.
	 */
	static public String readFileAsString(final File f) throws Exception {
		StringBuilder sb = new StringBuilder((int) f.length() + 20);
		readFileAsString(sb, f);
		return sb.toString();
	}

	static public void readFileAsString(final Appendable o, final File f) throws Exception {
		try(LineNumberReader lr = new LineNumberReader(new FileReader(f))) {
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
		try(InputStream is = new FileInputStream(srcf); OutputStream os = new FileOutputStream(destf)) {
			copyFile(os, is, maxSize);
			ignore(destf.setLastModified(srcf.lastModified()));
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
}

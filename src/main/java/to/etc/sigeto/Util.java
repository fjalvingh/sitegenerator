package to.etc.sigeto;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

public class Util {
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


}

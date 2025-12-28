package to.etc.sigeto.emojis;

import org.eclipse.jdt.annotation.Nullable;
import to.etc.sigeto.unidiot.WrappedException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Load all empji resources from the resource file.
 */
final public class EmojiRegistry {
	static private final Map<String, EmojiRef> m_refMap = new HashMap<>();

	private static final String GITHUB_ROOT = "https://github.githubassets.com/images/icons/emoji/";

	private EmojiRegistry() {
		//-- Empty
	}

	static {
		initialize();
	}

	static private void initialize() {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(EmojiRegistry.class.getResourceAsStream("/emoji/emoji.csv")))) {
			br.readLine();								// Skip column names

			String line;
			while(null != (line = br.readLine())) {
				String[] fields = line.split("\\|", -1);
				String shortcut = getField(fields, 0);
				String category = getField(fields, 1);				// Always github?
				String cheat = getField(fields, 2);
				String ghFile = getField(fields, 3);
				String uniChars = getField(fields, 4);
				String aliasStr = getField(fields, 8);

				EmojiRef er = null;
				if(null != ghFile) {
					er = new EmojiRef(GITHUB_ROOT, ghFile);
				}
				if(null != er) {
					if(null != shortcut)
						m_refMap.put(shortcut, er);
					if(null != aliasStr) {
						for(String s : aliasStr.split(",")) {
							m_refMap.put(s, er);
						}
					}
				}
			}
		} catch(Exception x) {
			throw WrappedException.wrap(x);
		}
	}

	@Nullable
	private static String getField(String[] in, int index) {
		if(index >= in.length)
			return null;
		String s = in[index];
		if(s.length() > 0 && s.charAt(0) == ' ')
			return null;
		return s;
	}

	@Nullable
	public static EmojiRef find(String code) {
		return m_refMap.get(code.toLowerCase());
	}

	static public void main(String[] args) throws Exception {
		initialize();

		EmojiRef emojiRef = m_refMap.get("laugh");


	}

	//static public void main(String[] args) throws Exception {
	//	try(BufferedReader br = new BufferedReader(new InputStreamReader(EmojiRegistry.class.getResourceAsStream("/emoji/emoji.txt")))) {
	//		try(FileWriter fw = new FileWriter(new File("/home/jal/emoji.csv"))) {
	//
	//			StringBuilder sb = new StringBuilder();
	//			String line;
	//			while(null != (line = br.readLine())) {
	//				sb.setLength(0);
	//				String[] fields = line.split("\t");
	//				for(int i = 0; i < fields.length; i++) {
	//					String s = getField(fields, i);
	//
	//					if(i != 0)
	//						sb.append("|");
	//					if(s != null)
	//						sb.append(s);
	//				}
	//				sb.append("\n");
	//				fw.write(sb.toString());
	//			}
	//		}
	//	}
	//}

}

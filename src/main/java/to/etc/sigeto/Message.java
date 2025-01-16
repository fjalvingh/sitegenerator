package to.etc.sigeto;

public class Message {
	private final ContentItem m_file;

	private final int m_lineNumber;

	private final MsgType m_type;

	private final String m_message;

	public Message(ContentItem file, int lineNumber, MsgType type, String message) {
		m_file = file;
		m_lineNumber = lineNumber;
		m_type = type;
		m_message = message;
	}

	public ContentItem getFile() {
		return m_file;
	}

	public int getLineNumber() {
		return m_lineNumber;
	}

	public MsgType getType() {
		return m_type;
	}

	public String getMessage() {
		return m_message;
	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(m_type.name());
		sb.append(" ");

		ContentItem file = m_file;
		if(null != file) {
			sb.append(file.getRelativePath());
			if(m_lineNumber > 0) {
				sb.append("(").append(m_lineNumber).append(") ");
			}
		}
		sb.append(m_message);
		return sb.toString();
	}
}

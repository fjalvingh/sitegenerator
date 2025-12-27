package to.etc.sigeto.notifications;

import org.commonmark.node.CustomBlock;

public class NotificationBlock extends CustomBlock {
	private Notification m_type;

	public NotificationBlock(Notification type) {
		m_type = type;
	}

	public Notification getType() {
		return m_type;
	}
}

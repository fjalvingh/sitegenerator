package to.etc.sigeto.notifications;

public enum Notification {
	INFO, SUCCESS, WARNING, ERROR;

	public static Notification fromString(String s) {
		if(s == null || s.trim().length() == 0) {
			return INFO;
		}

		switch(s){
			case "v":
				return SUCCESS;
			case "x", "e":
				return ERROR;
			case "!", "w":
				return WARNING;
			case "i":
				return INFO;
			default:
				throw new IllegalStateException();
		}
	}
}

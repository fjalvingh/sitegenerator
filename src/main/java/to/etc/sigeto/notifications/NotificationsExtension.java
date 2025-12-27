package to.etc.sigeto.notifications;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

final public class NotificationsExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	//private final DomElementMapper domElementMapper;
	//private final ClassMapper classMapper;

	private NotificationsExtension() {
	}

	public static NotificationsExtension create() {
		return new NotificationsExtension();
	}

	@Override
	public void extend(org.commonmark.parser.Parser.Builder parserBuilder) {
		parserBuilder.customBlockParserFactory(new NotificationBlockParser.Factory());
	}

	@Override
	public void extend(org.commonmark.renderer.html.HtmlRenderer.Builder htmlBuilder) {
		htmlBuilder.nodeRendererFactory(context -> new NotificationNodeRenderer(context));
	}
}

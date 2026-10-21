package to.etc.sigeto;

public enum ContentFileType {
	Markdown,
	Image,
	Resource,

	/** A page the generator makes itself, like the tag page: it has no source file. */
	Generated
}

package org.freeplane.api;

/** Node's font: <code>node.style.font</code> - read-only. */
public interface FontRO {
	String getName();

	int getSize();

	boolean isBold();

	boolean isBoldSet();

	boolean isItalic();

	boolean isItalicSet();

	boolean isUnderline();

	boolean isUnderlineSet();

	@Deprecated
	boolean isStrikedThrough();

	boolean isStrikethrough();

	@Deprecated
	boolean isStrikedThroughSet();

	boolean isStrikethroughSet();

	boolean isNameSet();

	boolean isSizeSet();
}

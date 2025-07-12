package org.freeplane.api;

/** Node's font: <code>node.style.font</code> - read-write. */
public interface Font extends FontRO {
	void resetBold();

	void resetItalic();

	@Deprecated
	void resetStrikedThrough();

	void resetStrikethrough();

	void resetName();

	void resetSize();

	void setBold(boolean bold);

	void setItalic(boolean italic);

	@Deprecated
	void setStrikedThrough(boolean strikedThrough);

	void setStrikethrough(boolean strikethrough);

	void setUnderline(boolean underline);

	void setName(String name);

	void setSize(int size);
}

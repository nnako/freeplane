/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks;

class NodeBookmarkDescriptor {
	private final String name;
	private final boolean opensAsRoot;

	NodeBookmarkDescriptor(String name, boolean opensAsRoot) {
		super();
		this.name = name;
		this.opensAsRoot = opensAsRoot;
	}

	public String getName() {
		return name;
	}

	boolean opensAsRoot() {
		return opensAsRoot;
	}
}

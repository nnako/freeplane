/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks;

import java.util.Objects;

public class NodeBookmarkDescriptor {
	private final String name;
	private final boolean opensAsRoot;

	public NodeBookmarkDescriptor(String name, boolean opensAsRoot) {
		this.name = name;
		this.opensAsRoot = opensAsRoot;
	}

	public String getName() {
		return name;
	}

	public boolean opensAsRoot() {
		return opensAsRoot;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, opensAsRoot);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeBookmarkDescriptor other = (NodeBookmarkDescriptor) obj;
		return Objects.equals(name, other.name) && opensAsRoot == other.opensAsRoot;
	}

	@Override
	public String toString() {
		return "NodeBookmarkDescriptor [name=" + name + ", opensAsRoot=" + opensAsRoot + "]";
	}

}

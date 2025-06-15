/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.api;

/**
 * Represents a bookmark for a specific node in the mind map.
 * Bookmarks provide quick navigation to important nodes and can be configured
 * to either select the node or open it as the root of the view.
 * 
 * @since 1.12.12
 */
public interface NodeBookmark {
	/** 
	 * Returns the node that this bookmark points to.
	 * @return the bookmarked node
	 */
	Node getNode();
	
	/** 
	 * Returns the display name of this bookmark.
	 * @return the bookmark name as shown in the UI
	 */
	String getName();
	
	/** 
	 * Returns the type of this bookmark, which determines its behavior when activated.
	 * @return the bookmark type (SELECT or ROOT)
	 * @see BookmarkType
	 */
	BookmarkType getBookmarkType();
}

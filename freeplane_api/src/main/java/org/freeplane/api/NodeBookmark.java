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
	BookmarkType getType();

	/**
	 * Opens this bookmark using its default behavior.
	 * The behavior is determined by the bookmark's type:
	 * - SELECT type: navigates to the node and selects it
	 * - ROOT type: opens the node as the root of the view
	 * @see #getType()
	 * @see #open(BookmarkType)
	 */
	void open();

	/**
	 * Opens this bookmark with the specified mode, overriding the bookmark's default type.
	 * @param mode the bookmark type to use when opening:
	 *             SELECT to navigate to and select the node,
	 *             ROOT to open the node as the root of the view
	 * @see BookmarkType
	 * @see #open()
	 */
	void open(BookmarkType mode);

	/**
	 * Opens this bookmark with the specified mode, overriding the bookmark's default type.
	 * Convenience overload that accepts mode as a string for easier scripting.
	 * @param mode the bookmark type as string - "SELECT" or "ROOT" (case insensitive)
	 * @throws IllegalArgumentException if mode is not a valid BookmarkType value
	 * @see BookmarkType
	 * @see #open()
	 * @see #open(BookmarkType)
	 */
	void open(String mode);
}

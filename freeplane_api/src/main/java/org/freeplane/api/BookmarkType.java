/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.api;

/**
 * Defines the behavior of a bookmark when activated.
 * 
 * @since 1.12.12
 */
public enum BookmarkType {
	/** When activated, the bookmarked node becomes the root of the view, 
	 * showing only this node and its descendants. */
	ROOT, 
	
	/** When activated, the bookmarked node is selected and the view 
	 * scrolls to make it visible, but the map's root remains unchanged. */
	SELECT
}

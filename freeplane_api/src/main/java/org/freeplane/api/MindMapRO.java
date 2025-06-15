package org.freeplane.api;

import java.awt.Color;
import java.io.File;
import java.net.URI;
import java.util.List;

/** The map a node belongs to: <code>node.map</code> - read-only.
 *
 * @since 1.7.10
 * */
public interface MindMapRO {
	/** @since 1.2 */
	Node getRoot();

	/** @deprecated since 1.2 - use {@link #getRoot()} instead. */
	@Deprecated
	Node getRootNode();

	/** get node by id.
	 * @return the node if the map contains it or null otherwise. */
	Node node(String id);

	/** returns the filenname of the map as a java.io.File object if available or null otherwise. */
	File getFile();

	/** returns the title of the MapView.
	 * @since 1.2 */
	String getName();

	/** @since 1.2 */
	boolean isSaved();

    /** @since 1.2 */
    Color getBackgroundColor();

    /** returns HTML color spec like #ff0000 (red) or #222222 (darkgray).
     *  @since 1.2 */
    String getBackgroundColorCode();

	/** @since 1.11.1 */
	ConditionalStyles getConditionalStyles();

	/** returns list with the user defined styles names of the map
	 * @since 1.11.8
	 * @return list of String
	 */
	List<String> getUserDefinedStylesNames();

	/** @return Followed-Map URI, as saved in .mm, or {@code null} if no mind map is followed
	 * @since 1.11.11 */
	URI getFollowedMap();

	/** @return Associated-Template URI, as saved in .mm, or {@code null} if no template is associated with the mind map
	 * @since 1.11.11 */
	URI getAssociatedTemplate();

	/** @return Followed-Map URI as File, with User- or Standard-Templates Directory resolved if necessary, or {@code null} if no mind map is followed
	 * @see MindMap#setFollowedMap(URI)
	 * @since 1.11.11 */
	File getFollowedMapFile();

	 /** @return Associated-Template URI as File, with User- or Standard-Templates Directory resolved if necessary, or {@code null} if no template is associated with the mind map
	  * @see MindMap#setAssociatedTemplate(URI)
	  * @since 1.11.11 */
	       File getAssociatedTemplateFile();

       /** Returns all bookmarks defined in this mind map.
        * Bookmarks provide quick access to important nodes in the map.
        * The returned list includes the automatically created root bookmark.
        * @return a list of all bookmarks in this map
        * @since 1.12.12 */
       List<NodeBookmark> getBookmarks();
}
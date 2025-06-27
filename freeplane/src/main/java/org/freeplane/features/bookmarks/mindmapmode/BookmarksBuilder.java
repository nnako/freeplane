/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

import java.io.IOException;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IElementHandler;
import org.freeplane.core.io.IExtensionElementWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.features.map.MapModel;
import org.freeplane.n3.nanoxml.XMLElement;

class BookmarksBuilder implements IExtensionElementWriter, IElementHandler {

	private static final String XML_BOOKMARKS = "bookmarks";
	private static final String XML_BOOKMARK = "bookmark";
	private static final String XML_NODE_ID = "nodeId";
	private static final String XML_NAME = "name";
	private static final String XML_OPENS_AS_ROOT = "opensAsRoot";

	public void registerBy(final ReadManager reader, final WriteManager writer) {
		reader.addElementHandler(XML_BOOKMARKS, this);
		reader.addElementHandler(XML_BOOKMARK, this);
		writer.addExtensionElementWriter(MapBookmarks.class, this);
	}


	@Override
	public Object createElement(Object parent, String tag, XMLElement attributes) {
		if (XML_BOOKMARKS.equals(tag) && parent instanceof MapModel) {
			final MapModel map = (MapModel) parent;
			final MapBookmarks mapBookmarks = new MapBookmarks(map);
			map.addExtension(mapBookmarks);
			return mapBookmarks;
		}

		if (attributes != null && XML_BOOKMARK.equals(tag) && parent instanceof MapBookmarks) {
			MapBookmarks bookmarks = (MapBookmarks) parent;
			String nodeId = attributes.getAttribute(XML_NODE_ID, null);
			String name = attributes.getAttribute(XML_NAME, null);
			String opensAsRootStr = attributes.getAttribute(XML_OPENS_AS_ROOT, "false");
			boolean opensAsRoot = Boolean.parseBoolean(opensAsRootStr);
			NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(name, opensAsRoot);
			bookmarks.add(nodeId, descriptor);
		}

		return null;
	}

	@Override
	public void writeContent(ITreeWriter writer, Object element, IExtension extension)
	        throws IOException {
		if (!(element instanceof MapModel)) {
			return;
		}

		MapBookmarks bookmarks = (MapBookmarks) extension;
		MapModel map = (MapModel) element;

		XMLElement bookmarksElement = new XMLElement(XML_BOOKMARKS);

		for (NodeBookmark bookmark : bookmarks.getBookmarks()) {
			if (map.getNodeForID(bookmark.getNode().getID()) != null) {
				XMLElement bookmarkElement = new XMLElement(XML_BOOKMARK);
				bookmarkElement.setAttribute(XML_NODE_ID, bookmark.getNode().getID());
				bookmarkElement.setAttribute(XML_NAME, bookmark.getDescriptor().getName());
				bookmarkElement.setAttribute(XML_OPENS_AS_ROOT, Boolean.toString(bookmark.getDescriptor().opensAsRoot()));
				bookmarksElement.addChild(bookmarkElement);
			}
		}
		writer.addElement(null, bookmarksElement);
	}
}

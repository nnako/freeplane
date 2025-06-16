/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

import java.io.IOException;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IElementDOMHandler;
import org.freeplane.core.io.IExtensionElementWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.features.edge.EdgeModel;
import org.freeplane.features.map.MapModel;
import org.freeplane.n3.nanoxml.XMLElement;

public class BookmarksBuilder implements IExtensionElementWriter, IElementDOMHandler {

	private static final String XML_BOOKMARKS = "bookmarks";
	private static final String XML_BOOKMARK = "bookmark";
	private static final String XML_NODE_ID = "nodeId";
	private static final String XML_NAME = "name";
	private static final String XML_OPENS_AS_ROOT = "opensAsRoot";

	public void registerBy(final ReadManager reader, final WriteManager writer) {
		reader.addElementHandler("edge", this);
		writer.addExtensionElementWriter(EdgeModel.class, this);
	}


	@Override
	public Object createElement(Object parent, String tag, XMLElement attributes) {
		if (!(parent instanceof MapModel)) {
			return null;
		}

		if (XML_BOOKMARKS.equals(tag)) {
			final MapModel map = (MapModel) parent;
			final MapBookmarks mapBookmarks = new MapBookmarks(map);
			map.addExtension(mapBookmarks);
			return mapBookmarks;
		}

		if (XML_BOOKMARK.equals(tag)) {
			return parent;
		}

		return null;
	}

	@Override
	public void endElement(Object parent, String tag, Object userObject, XMLElement dom) {
		if (!(parent instanceof MapModel) || !XML_BOOKMARK.equals(tag)) {
			return;
		}

		MapModel map = (MapModel) parent;
		String nodeId = dom.getAttribute(XML_NODE_ID, null);
		String name = dom.getAttribute(XML_NAME, null);
		String opensAsRootStr = dom.getAttribute(XML_OPENS_AS_ROOT, "false");
		boolean opensAsRoot = Boolean.parseBoolean(opensAsRootStr);
		NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(name, opensAsRoot);

		MapBookmarks bookmarks = MapBookmarks.of(map);
		bookmarks.add(nodeId, descriptor);
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
		writer.addElement(null, bookmarksElement);

		for (NodeBookmark bookmark : bookmarks.getBookmarks()) {
			if (map.getNodeForID(bookmark.getNode().getID()) != null) {
				XMLElement bookmarkElement = new XMLElement(XML_BOOKMARK);
				bookmarkElement.setAttribute(XML_NODE_ID, bookmark.getNode().getID());
				bookmarkElement.setAttribute(XML_NAME, bookmark.getDescriptor().getName());
				bookmarkElement.setAttribute(XML_OPENS_AS_ROOT, Boolean.toString(bookmark.getDescriptor().opensAsRoot()));
				writer.addElement(null, bookmarkElement);
			}
		}
	}
}

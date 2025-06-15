/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;

public class MapBookmarks implements IExtension {
	private MapModel map;
	private List<String> nodeIDs;
	private Map<String, NodeBookmarkDescriptor> bookmarks;

	public static MapBookmarks of(MapModel map) {
		MapBookmarks bookmarks = map.getExtension(MapBookmarks.class);
		if(bookmarks == null) {
			bookmarks = new MapBookmarks(map);
			bookmarks.add(map.getRootNode().getID(), new NodeBookmarkDescriptor(TextUtils.getRawText("AutomaticLayout.level.root"), true));
			map.addExtension(bookmarks);
		}
		return bookmarks;
	}

	MapBookmarks(MapModel map) {
		super();
		this.map = map;
		this.nodeIDs = new ArrayList<String>();
		this.bookmarks = new HashMap<String, NodeBookmarkDescriptor>();
	}

	void add(String id, NodeBookmarkDescriptor bookmark) {
		if (bookmarks.put(id, bookmark) == null) {
			nodeIDs.add(id);
		}
	}

	boolean remove(String id) {
		if (id == null) {
			return false;
		}
		if (bookmarks.remove(id) != null) {
			nodeIDs.remove(id);
			return true;
		}
		return false;
	}

	boolean move(String id, int indexInVisibleList) {
		if (isInvalidMoveRequest(id, indexInVisibleList)) {
			return false;
		}

		List<String> visibleNodeIDs = getValidNodeIDsInOrder();

		if (isIndexOutOfBounds(indexInVisibleList, visibleNodeIDs) || isNodeNotVisible(id, visibleNodeIDs)) {
			return false;
		}

		if (isAlreadyAtTargetPosition(id, indexInVisibleList, visibleNodeIDs)) {
			return true;
		}

		reorderVisibleNodes(id, indexInVisibleList, visibleNodeIDs);
		rebuildInternalNodeList(visibleNodeIDs);
		return true;
	}

	private boolean isInvalidMoveRequest(String id, int index) {
		return id == null || !bookmarks.containsKey(id) || map.getNodeForID(id) == null;
	}

	private List<String> getValidNodeIDsInOrder() {
		return nodeIDs.stream()
			.filter(nodeId -> map.getNodeForID(nodeId) != null)
			.collect(Collectors.toList());
	}

	private boolean isIndexOutOfBounds(int index, List<String> visibleNodes) {
		return index < 0 || index >= visibleNodes.size();
	}

	private boolean isNodeNotVisible(String id, List<String> visibleNodes) {
		return !visibleNodes.contains(id);
	}

	private boolean isAlreadyAtTargetPosition(String id, int targetIndex, List<String> visibleNodes) {
		return visibleNodes.indexOf(id) == targetIndex;
	}

	private void reorderVisibleNodes(String id, int targetIndex, List<String> visibleNodes) {
		int currentIndex = visibleNodes.indexOf(id);
		visibleNodes.remove(currentIndex);
		visibleNodes.add(targetIndex, id);
	}

	private void rebuildInternalNodeList(List<String> reorderedVisibleNodes) {
		List<String> newNodeIDs = new ArrayList<>();
		int visibleIndex = 0;

		for (String nodeId : nodeIDs) {
			if (map.getNodeForID(nodeId) != null) {
				newNodeIDs.add(reorderedVisibleNodes.get(visibleIndex++));
			} else {
				newNodeIDs.add(nodeId);
			}
		}

		nodeIDs = newNodeIDs;
	}

	public NodeBookmark getBookmark(String id) {
		final NodeBookmarkDescriptor descriptor = bookmarks.get(id);
		if(descriptor == null)
			return null;
		final NodeModel node = map.getNodeForID(id);
		if(node == null)
			return null;
		return new NodeBookmark(node, descriptor);
	}

	public List<String> getNodeIDs() {
		return nodeIDs.stream().filter(id -> map.getNodeForID(id) != null).collect(Collectors.toList());
	}

	public List<NodeBookmark> getBookmarks() {
		return nodeIDs.stream()
				.map(this::getBookmark)
				.filter(x -> x != null)
				.collect(Collectors.toList());
	}

	public MapModel getMap() {
		return map;
	}

	public int size() {
		return bookmarks.size();
	}

	public boolean contains(String id) {
		return bookmarks.containsKey(id);
	}

	public boolean clear() {
		if(bookmarks.isEmpty())
			return false;
		bookmarks.clear();
		nodeIDs.clear();
		return true;
	}
}

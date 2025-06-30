/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

public class MapBookmarks implements IExtension {
	private MapModel map;
	private List<String> nodeIDs;
	private Map<String, NodeBookmarkDescriptor> bookmarks;
	private Map<String, String> selectedNodesBySelectionRoot;

	static MapBookmarks of(MapModel map) {
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
		this.nodeIDs = new ArrayList<>();
		this.bookmarks = new HashMap<>();
		this.selectedNodesBySelectionRoot = new HashMap<>();
	}

	void add(String id, NodeBookmarkDescriptor bookmark) {
		if (bookmarks.put(id, bookmark) == null) {
			nodeIDs.add(id);
		}
	}

	void addAtPosition(String id, NodeBookmarkDescriptor bookmark, int position) {
		if (bookmarks.put(id, bookmark) == null) {
			List<String> visibleBookmarkIds = getVisibleBookmarkIds();
			int insertPosition = Math.max(0, Math.min(position, visibleBookmarkIds.size()));
			
			if (insertPosition >= visibleBookmarkIds.size()) {
				nodeIDs.add(id);
			} else {
				String targetNodeId = visibleBookmarkIds.get(insertPosition);
				int targetIndex = nodeIDs.indexOf(targetNodeId);
				nodeIDs.add(targetIndex, id);
			}
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
		if (!bookmarkExists(id)) {
			return false;
		}

		List<String> visibleBookmarks = getVisibleBookmarkIds();

		if (isIndexOutOfBounds(indexInVisibleList, visibleBookmarks) || !isBookmarkVisible(id, visibleBookmarks)) {
			return false;
		}

		if (isBookmarkAtTargetPosition(id, indexInVisibleList, visibleBookmarks)) {
			return true;
		}

		moveBookmarkInList(id, indexInVisibleList, visibleBookmarks);
		updateNodeIdList(visibleBookmarks);
		return true;
	}

	private boolean bookmarkExists(String id) {
		return id != null && bookmarks.containsKey(id) && map.getNodeForID(id) != null;
	}

	private boolean isBookmarkVisible(String id, List<String> visibleNodes) {
		return visibleNodes.contains(id);
	}

	private List<String> getVisibleBookmarkIds() {
		return nodeIDs.stream()
			.filter(nodeId -> map.getNodeForID(nodeId) != null)
			.collect(Collectors.toList());
	}

	private boolean isIndexOutOfBounds(int index, List<String> visibleNodes) {
		return index < 0 || index >= visibleNodes.size();
	}

	private boolean isBookmarkAtTargetPosition(String id, int targetIndex, List<String> visibleBookmarks) {
		return visibleBookmarks.indexOf(id) == targetIndex;
	}

	private void moveBookmarkInList(String id, int targetIndex, List<String> visibleBookmarks) {
		int currentIndex = visibleBookmarks.indexOf(id);
		visibleBookmarks.remove(currentIndex);
		visibleBookmarks.add(targetIndex, id);
	}

	private void updateNodeIdList(List<String> reorderedVisibleBookmarks) {
		List<String> newNodeIDs = new ArrayList<>();
		int visibleIndex = 0;

		for (String nodeId : nodeIDs) {
			if (map.getNodeForID(nodeId) != null) {
				newNodeIDs.add(reorderedVisibleBookmarks.get(visibleIndex++));
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

	int size() {
		return bookmarks.size();
	}

	boolean contains(String id) {
		return bookmarks.containsKey(id);
	}

	boolean clear() {
		if(bookmarks.isEmpty())
			return false;
		bookmarks.clear();
		nodeIDs.clear();
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bookmarks, map, nodeIDs);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapBookmarks other = (MapBookmarks) obj;
		return Objects.equals(bookmarks, other.bookmarks) && Objects.equals(map, other.map)
		        && Objects.equals(nodeIDs, other.nodeIDs);
	}

	boolean opensAsRoot(NodeModel node) {
		final NodeBookmarkDescriptor descriptor = bookmarks.get(node.getID());
		return descriptor != null && descriptor.opensAsRoot();
	}

	void onSelect(NodeModel node) {
		final IMapSelection selection = Controller.getCurrentController().getSelection();
		final String rootId = selection.getSelectionRoot().getID();
		final String nodeId = node.createID();
		selectedNodesBySelectionRoot.put(rootId, nodeId);
	}

	NodeModel getSelectedNodeForRoot(NodeModel node) {
		final String rootId = node.getID();
		final String selectedNodeId = selectedNodesBySelectionRoot.getOrDefault(rootId, rootId);
		if(selectedNodeId.equals(rootId))
			return node;
		final NodeModel selectedNode = map.getNodeForID(selectedNodeId);
		return selectedNode != null ? selectedNode : node;
	}


}

/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.any;

import java.util.List;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.mockito.MockedStatic;

public class MapBookmarksTest {
	
	private MapBookmarks mapBookmarks;
	private MapModel mapModel;
	private NodeModel rootNode;
	private NodeModel node1;
	private NodeModel node2;
	private NodeModel node3;
	private NodeBookmarkDescriptor bookmark1;
	private NodeBookmarkDescriptor bookmark2;
	private NodeBookmarkDescriptor bookmark3;
	private MockedStatic<TextUtils> textUtilsMock;
	
	@Before
	public void setUp() {
		textUtilsMock = mockStatic(TextUtils.class);
		textUtilsMock.when(() -> TextUtils.getRawText("AutomaticLayout.level.root")).thenReturn("Root");
		
		mapModel = mock(MapModel.class);
		rootNode = mock(NodeModel.class);
		node1 = mock(NodeModel.class);
		node2 = mock(NodeModel.class);
		node3 = mock(NodeModel.class);
		
		when(mapModel.getRootNode()).thenReturn(rootNode);
		when(rootNode.getID()).thenReturn("root");
		when(mapModel.getNodeForID("root")).thenReturn(rootNode);
		when(mapModel.getNodeForID("node1")).thenReturn(node1);
		when(mapModel.getNodeForID("node2")).thenReturn(node2);
		when(mapModel.getNodeForID("node3")).thenReturn(node3);
		when(mapModel.getNodeForID("nonexistent")).thenReturn(null);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(null);
		doNothing().when(mapModel).addExtension(any(MapBookmarks.class));
		
		mapBookmarks = MapBookmarks.of(mapModel);
		bookmark1 = new NodeBookmarkDescriptor("Bookmark 1", false);
		bookmark2 = new NodeBookmarkDescriptor("Bookmark 2", true);
		bookmark3 = new NodeBookmarkDescriptor("Bookmark 3", false);
	}
	
	@After
	public void tearDown() {
		if (textUtilsMock != null) {
			textUtilsMock.close();
		}
	}
	
	@Test
	public void shouldInitializeWithRootBookmark() {
		assertThat(mapBookmarks.size(), equalTo(1));
		assertThat(mapBookmarks.getNodeIDs().size(), equalTo(1));
		assertThat(mapBookmarks.contains("root"), equalTo(true));
	}
	
	@Test
	public void shouldReturnCorrectMap() {
		assertThat(mapBookmarks.getMap(), equalTo(mapModel));
	}
	
	@Test
	public void shouldAddBookmarkSuccessfully() {
		boolean result = mapBookmarks.add("node1", bookmark1);
		
		assertThat(result, equalTo(true));
		assertThat(mapBookmarks.size(), equalTo(2));
		assertThat(mapBookmarks.contains("node1"), equalTo(true));
		
		NodeBookmark retrievedBookmark = mapBookmarks.getBookmark("node1");
		assertThat(retrievedBookmark != null, equalTo(true));
		
		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.size(), equalTo(2));
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node1"));
	}
	
	@Test
	public void shouldReturnFalseWhenAddingExistingBookmark() {
		mapBookmarks.add("node1", bookmark1);
		boolean result = mapBookmarks.add("node1", bookmark2);
		
		assertThat(result, equalTo(false));
		assertThat(mapBookmarks.size(), equalTo(2));
		NodeBookmark retrievedBookmark = mapBookmarks.getBookmark("node1");
		assertThat(retrievedBookmark != null, equalTo(true));
	}
	
	@Test
	public void shouldReturnFalseWhenAddingNullId() {
		boolean result = mapBookmarks.add(null, bookmark1);
		
		assertThat(result, equalTo(false));
		assertThat(mapBookmarks.size(), equalTo(1));
	}
	
	@Test
	public void shouldReturnFalseWhenAddingNullBookmark() {
		boolean result = mapBookmarks.add("node1", null);
		
		assertThat(result, equalTo(false));
		assertThat(mapBookmarks.size(), equalTo(1));
	}
	
	@Test
	public void shouldAddMultipleBookmarks() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);
		mapBookmarks.add("node3", bookmark3);
		
		assertThat(mapBookmarks.size(), equalTo(4));
		
		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.size(), equalTo(4));
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node1"));
		assertThat(nodeIDs.get(2), equalTo("node2"));
		assertThat(nodeIDs.get(3), equalTo("node3"));
	}
	
	@Test
	public void shouldRemoveBookmarkSuccessfully() {
		mapBookmarks.add("node1", bookmark1);
		boolean result = mapBookmarks.remove("node1");
		
		assertThat(result, equalTo(true));
		assertThat(mapBookmarks.size(), equalTo(1));
		assertThat(mapBookmarks.contains("node1"), equalTo(false));
		assertThat(mapBookmarks.getBookmark("node1"), nullValue());
	}
	
	@Test
	public void shouldNotRemoveRootBookmark() {
		boolean result = mapBookmarks.remove("root");
		
		assertThat(result, equalTo(false));
		assertThat(mapBookmarks.size(), equalTo(1));
		assertThat(mapBookmarks.contains("root"), equalTo(true));
	}
	
	@Test
	public void shouldReturnFalseWhenRemovingNonExistentBookmark() {
		boolean result = mapBookmarks.remove("nonexistent");
		
		assertThat(result, equalTo(false));
		assertThat(mapBookmarks.size(), equalTo(1));
	}
	
	@Test
	public void shouldReturnFalseWhenRemovingNullId() {
		boolean result = mapBookmarks.remove(null);
		
		assertThat(result, equalTo(false));
	}
	
	@Test
	public void shouldRemoveCorrectBookmarkFromMultiple() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);
		mapBookmarks.add("node3", bookmark3);
		
		boolean result = mapBookmarks.remove("node2");
		
		assertThat(result, equalTo(true));
		assertThat(mapBookmarks.size(), equalTo(3));
		assertThat(mapBookmarks.contains("node2"), equalTo(false));
		assertThat(mapBookmarks.contains("root"), equalTo(true));
		assertThat(mapBookmarks.contains("node1"), equalTo(true));
		assertThat(mapBookmarks.contains("node3"), equalTo(true));
		
		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.size(), equalTo(3));
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node1"));
		assertThat(nodeIDs.get(2), equalTo("node3"));
	}
	
	@Test
	public void shouldMoveBookmarkToNewPosition() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);
		mapBookmarks.add("node3", bookmark3);
		
		boolean result = mapBookmarks.move("node1", 3);
		
		assertThat(result, equalTo(true));
		
		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node2"));
		assertThat(nodeIDs.get(2), equalTo("node3"));
		assertThat(nodeIDs.get(3), equalTo("node1"));
	}
	
	@Test
	public void shouldReturnTrueWhenMovingToSamePosition() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);

		boolean result = mapBookmarks.move("root", 0);

		assertThat(result, equalTo(true));

		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node1"));
		assertThat(nodeIDs.get(2), equalTo("node2"));
	}

	@Test
	public void shouldReturnFalseWhenMovingNonExistentBookmark() {
		mapBookmarks.add("node1", bookmark1);

		boolean result = mapBookmarks.move("nonexistent", 0);

		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldReturnFalseWhenMovingWithNullId() {
		mapBookmarks.add("node1", bookmark1);

		boolean result = mapBookmarks.move(null, 0);

		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldReturnFalseWhenMovingToNegativeIndex() {
		mapBookmarks.add("node1", bookmark1);

		boolean result = mapBookmarks.move("node1", -1);

		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldReturnFalseWhenMovingToIndexOutOfBounds() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);

		boolean result = mapBookmarks.move("node1", 3);

		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldClearAllBookmarks() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);

		mapBookmarks.clear();

		assertThat(mapBookmarks.size(), equalTo(0));
		assertThat(mapBookmarks.getNodeIDs().isEmpty(), equalTo(true));
		assertThat(mapBookmarks.contains("root"), equalTo(false));
		assertThat(mapBookmarks.contains("node1"), equalTo(false));
		assertThat(mapBookmarks.contains("node2"), equalTo(false));
	}

	@Test
	public void shouldReturnDefensiveCopyOfNodeIDs() {
		mapBookmarks.add("node1", bookmark1);
		List<String> nodeIDs1 = mapBookmarks.getNodeIDs();
		List<String> nodeIDs2 = mapBookmarks.getNodeIDs();
		
		assertThat(nodeIDs1 != nodeIDs2, equalTo(true));
		assertThat(nodeIDs1, equalTo(nodeIDs2));
		
		nodeIDs1.clear();
		assertThat(mapBookmarks.size(), equalTo(2));
		assertThat(mapBookmarks.getNodeIDs().size(), equalTo(2));
	}

	@Test
	public void shouldMaintainOrderWhenMovingMultipleItems() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);
		mapBookmarks.add("node3", bookmark3);
		
		mapBookmarks.move("node3", 1);
		
		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node3"));
		assertThat(nodeIDs.get(2), equalTo("node1"));
		assertThat(nodeIDs.get(3), equalTo("node2"));
		
		mapBookmarks.move("node1", 3);
		
		nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.get(0), equalTo("root"));
		assertThat(nodeIDs.get(1), equalTo("node3"));
		assertThat(nodeIDs.get(2), equalTo("node2"));
		assertThat(nodeIDs.get(3), equalTo("node1"));
	}
	
	@Test
	public void shouldReturnBookmarks() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("node2", bookmark2);
		
		List<NodeBookmark> bookmarks = mapBookmarks.getBookmarks();
		
		assertThat(bookmarks.size(), equalTo(3));
		assertThat(bookmarks.get(0) != null, equalTo(true));
		assertThat(bookmarks.get(1) != null, equalTo(true));
		assertThat(bookmarks.get(2) != null, equalTo(true));
	}
	
	@Test
	public void shouldFilterNonExistentNodesFromGetNodeIDs() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("nonexistent", bookmark2);
		mapBookmarks.add("node2", bookmark3);
		
		assertThat(mapBookmarks.size(), equalTo(4));
		
		List<String> nodeIDs = mapBookmarks.getNodeIDs();
		assertThat(nodeIDs.size(), equalTo(3));
		assertThat(nodeIDs.contains("root"), equalTo(true));
		assertThat(nodeIDs.contains("node1"), equalTo(true));
		assertThat(nodeIDs.contains("node2"), equalTo(true));
		assertThat(nodeIDs.contains("nonexistent"), equalTo(false));
	}
	
	@Test
	public void shouldFilterNonExistentNodesFromGetBookmarks() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("nonexistent", bookmark2);
		mapBookmarks.add("node2", bookmark3);
		
		List<NodeBookmark> bookmarks = mapBookmarks.getBookmarks();
		
		assertThat(bookmarks.size(), equalTo(3));
		assertThat(bookmarks.get(0) != null, equalTo(true));
		assertThat(bookmarks.get(1) != null, equalTo(true));
		assertThat(bookmarks.get(2) != null, equalTo(true));
	}
	
	@Test
	public void shouldReturnNullBookmarkForNonExistentNode() {
		mapBookmarks.add("nonexistent", bookmark1);
		
		NodeBookmark bookmark = mapBookmarks.getBookmark("nonexistent");
		
		assertThat(bookmark, nullValue());
	}
	
	@Test
	public void shouldReturnNullBookmarkForUnknownId() {
		NodeBookmark bookmark = mapBookmarks.getBookmark("unknown");
		
		assertThat(bookmark, nullValue());
	}

	@Test
	public void shouldMoveWithCorrectIndexSemantics() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("invalid1", bookmark2);
		mapBookmarks.add("node2", bookmark3);
		mapBookmarks.add("invalid2", bookmark1);
		mapBookmarks.add("node3", bookmark2);
		
		when(mapModel.getNodeForID("invalid1")).thenReturn(null);
		when(mapModel.getNodeForID("invalid2")).thenReturn(null);
		
		List<String> visibleNodes = mapBookmarks.getNodeIDs();
		assertThat(visibleNodes.size(), equalTo(4));
		assertThat(visibleNodes.get(0), equalTo("root"));
		assertThat(visibleNodes.get(1), equalTo("node1"));
		assertThat(visibleNodes.get(2), equalTo("node2"));
		assertThat(visibleNodes.get(3), equalTo("node3"));
		
		boolean result = mapBookmarks.move("node1", 3);
		assertThat(result, equalTo(true));
		
		List<String> newOrder = mapBookmarks.getNodeIDs();
		assertThat(newOrder.size(), equalTo(4));
		assertThat(newOrder.get(0), equalTo("root"));
		assertThat(newOrder.get(1), equalTo("node2"));
		assertThat(newOrder.get(2), equalTo("node3"));
		assertThat(newOrder.get(3), equalTo("node1"));
	}
	
	@Test
	public void shouldRejectMoveToInvalidIndexInFilteredList() {
		mapBookmarks.add("node1", bookmark1);
		mapBookmarks.add("invalid", bookmark2);
		mapBookmarks.add("node2", bookmark3);
		
		when(mapModel.getNodeForID("invalid")).thenReturn(null);
		
		assertThat(mapBookmarks.getNodeIDs().size(), equalTo(3));
		
		boolean result = mapBookmarks.move("node1", 3);
		assertThat(result, equalTo(false));
		
		result = mapBookmarks.move("node1", 4);
		assertThat(result, equalTo(false));
	}
	
	@Test
	public void shouldRejectMoveOfInvalidNode() {
		mapBookmarks.add("invalid", bookmark1);
		
		when(mapModel.getNodeForID("invalid")).thenReturn(null);
		
		boolean result = mapBookmarks.move("invalid", 0);
		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldReturnExistingInstanceFromFactory() {
		MapBookmarks existing = mapBookmarks;
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(existing);
		
		MapBookmarks retrieved = MapBookmarks.of(mapModel);
		
		assertThat(retrieved, equalTo(existing));
	}
}
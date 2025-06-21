package org.freeplane.features.bookmarks.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.n3.nanoxml.XMLElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

public class BookmarksBuilderTest {

	private BookmarksBuilder bookmarksBuilder;
	private MapModel mapModel;
	private NodeModel rootNode;
	private NodeModel validNode;
	private ITreeWriter writer;
	private MockedStatic<TextUtils> textUtilsMock;
	private MapBookmarks mapBookmarks;

	@Before
	public void setUp() {
		textUtilsMock = mockStatic(TextUtils.class);
		textUtilsMock.when(() -> TextUtils.getRawText("AutomaticLayout.level.root")).thenReturn("Root");

		bookmarksBuilder = new BookmarksBuilder();
		mapModel = mock(MapModel.class);
		rootNode = mock(NodeModel.class);
		validNode = mock(NodeModel.class);
		writer = mock(ITreeWriter.class);
		mapBookmarks = mock(MapBookmarks.class);

		when(mapModel.getRootNode()).thenReturn(rootNode);
		when(rootNode.getID()).thenReturn("root");
		when(validNode.getID()).thenReturn("validNode");
		when(mapModel.getNodeForID("root")).thenReturn(rootNode);
		when(mapModel.getNodeForID("validNode")).thenReturn(validNode);
		when(mapModel.getNodeForID("invalidNode")).thenReturn(null);
		doNothing().when(mapModel).addExtension(any(MapBookmarks.class));
		doNothing().when(mapBookmarks).add(any(String.class), any(NodeBookmarkDescriptor.class));
	}

	@After
	public void tearDown() {
		if (textUtilsMock != null) {
			textUtilsMock.close();
		}
	}

	@Test
	public void shouldReturnNullForNonMapModelParent() {
		Object result = bookmarksBuilder.createElement("notAMapModel", "bookmarks", null);

		assertThat(result).isNull();
	}

	@Test
	public void shouldCreateAndAddMapBookmarksForBookmarksTag() {
		Object result = bookmarksBuilder.createElement(mapModel, "bookmarks", null);

		assertThat(result).isInstanceOf(MapBookmarks.class);
		verify(mapModel).addExtension(any(MapBookmarks.class));
	}

	@Test
	public void shouldReturnNullForBookmarkTagWithNonMapBookmarksParent() {
		XMLElement attributes = new XMLElement("bookmark");
		attributes.setAttribute("nodeId", "validNode");
		attributes.setAttribute("name", "Test Bookmark");

		Object result = bookmarksBuilder.createElement(mapModel, "bookmark", attributes);

		assertThat(result).isNull();
	}

	@Test
	public void shouldReturnNullForBookmarkTagWithMapBookmarksParentButNoAttributes() {
		Object result = bookmarksBuilder.createElement(mapBookmarks, "bookmark", null);

		assertThat(result).isNull();
	}

	@Test
	public void shouldAddBookmarkForBookmarkTagWithMapBookmarksParent() {
		XMLElement attributes = new XMLElement("bookmark");
		attributes.setAttribute("nodeId", "validNode");
		attributes.setAttribute("name", "Test Bookmark");
		attributes.setAttribute("opensAsRoot", "true");

		Object result = bookmarksBuilder.createElement(mapBookmarks, "bookmark", attributes);

		assertThat(result).isNull();
		verify(mapBookmarks).add(eq("validNode"), any(NodeBookmarkDescriptor.class));
	}

	@Test
	public void shouldDefaultOpensAsRootToFalseWhenNotSpecified() {
		XMLElement attributes = new XMLElement("bookmark");
		attributes.setAttribute("nodeId", "validNode");
		attributes.setAttribute("name", "Test Bookmark");

		bookmarksBuilder.createElement(mapBookmarks, "bookmark", attributes);

		verify(mapBookmarks).add(eq("validNode"), eq(new NodeBookmarkDescriptor("Test Bookmark", false)));
	}

	@Test
	public void shouldReturnNullForUnknownTag() {
		Object result = bookmarksBuilder.createElement(mapModel, "unknown", null);

		assertThat(result).isNull();
	}

	@Test
	public void shouldWriteContentOnlyForValidNodes() throws IOException {
		MapBookmarks realBookmarks = new MapBookmarks(mapModel);
		NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor("Test", false);
		realBookmarks.add("validNode", descriptor);

		bookmarksBuilder.writeContent(writer, mapModel, realBookmarks);

		verify(writer).addElement(eq(null), any(XMLElement.class));
	}

	@Test
	public void shouldCreateCorrectXMLStructureWhenWritingContent() throws IOException {
		MapBookmarks realBookmarks = new MapBookmarks(mapModel);
		NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor("Test Bookmark", true);
		realBookmarks.add("validNode", descriptor);
		when(validNode.getID()).thenReturn("validNode");

		bookmarksBuilder.writeContent(writer, mapModel, realBookmarks);

		ArgumentCaptor<XMLElement> captor = ArgumentCaptor.forClass(XMLElement.class);
		verify(writer).addElement(eq(null), captor.capture());

		XMLElement bookmarksElement = captor.getValue();
		assertThat(bookmarksElement.getName()).isEqualTo("bookmarks");
		assertThat(bookmarksElement.getChildren()).hasSize(1);

		XMLElement bookmarkElement = (XMLElement) bookmarksElement.getChildren().get(0);
		assertThat(bookmarkElement.getName()).isEqualTo("bookmark");
		assertThat(bookmarkElement.getAttribute("nodeId", null)).isEqualTo("validNode");
		assertThat(bookmarkElement.getAttribute("name", null)).isEqualTo("Test Bookmark");
		assertThat(bookmarkElement.getAttribute("opensAsRoot", null)).isEqualTo("true");
	}

	@Test
	public void shouldAddBookmarksFromXmlAndCheckContent() {
		// Given
		BookmarksBuilder builder = new BookmarksBuilder();
		MapModel map = mock(MapModel.class);
		NodeModel node = mock(NodeModel.class);
		when(node.getID()).thenReturn("node1");
		when(map.getNodeForID("node1")).thenReturn(node);
		when(map.getRootNode()).thenReturn(node);

		// When: create MapBookmarks from XML
		Object mapBookmarksObj = builder.createElement(map, "bookmarks", null);
		assertThat(mapBookmarksObj).isInstanceOf(MapBookmarks.class);
		MapBookmarks mapBookmarks = (MapBookmarks) mapBookmarksObj;

		// And: add a bookmark from XML
		XMLElement attributes = new XMLElement("bookmark");
		attributes.setAttribute("nodeId", "node1");
		attributes.setAttribute("name", "Bookmark One");
		attributes.setAttribute("opensAsRoot", "true");
		builder.createElement(mapBookmarks, "bookmark", attributes);

		// Then: MapBookmarks contains the bookmark with correct content
		assertThat(mapBookmarks.contains("node1")).isTrue();
		NodeBookmark bookmark = mapBookmarks.getBookmark("node1");
		assertThat(bookmark).isNotNull();
		assertThat(bookmark.getDescriptor().getName()).isEqualTo("Bookmark One");
		assertThat(bookmark.getDescriptor().opensAsRoot()).isTrue();
	}
}
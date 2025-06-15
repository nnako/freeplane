package org.freeplane.features.bookmarks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import org.mockito.MockedStatic;

public class BookmarksBuilderTest {

	private BookmarksBuilder bookmarksBuilder;
	private MapModel mapModel;
	private NodeModel rootNode;
	private NodeModel validNode;
	private ITreeWriter writer;
	private MockedStatic<TextUtils> textUtilsMock;

	@Before
	public void setUp() {
		textUtilsMock = mockStatic(TextUtils.class);
		textUtilsMock.when(() -> TextUtils.getRawText("AutomaticLayout.level.root")).thenReturn("Root");

		bookmarksBuilder = new BookmarksBuilder();
		mapModel = mock(MapModel.class);
		rootNode = mock(NodeModel.class);
		validNode = mock(NodeModel.class);
		writer = mock(ITreeWriter.class);

		when(mapModel.getRootNode()).thenReturn(rootNode);
		when(rootNode.getID()).thenReturn("root");
		when(validNode.getID()).thenReturn("validNode");
		when(mapModel.getNodeForID("root")).thenReturn(rootNode);
		when(mapModel.getNodeForID("validNode")).thenReturn(validNode);
		when(mapModel.getNodeForID("invalidNode")).thenReturn(null);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(null);
		doNothing().when(mapModel).addExtension(any(MapBookmarks.class));
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

		assertThat(result, nullValue());
	}

	@Test
	public void shouldCreateMapBookmarksForBookmarksTag() {
		Object result = bookmarksBuilder.createElement(mapModel, "bookmarks", null);

		assertThat(result instanceof MapBookmarks, equalTo(true));
	}

	@Test
	public void shouldReturnParentForBookmarkTag() {
		Object result = bookmarksBuilder.createElement(mapModel, "bookmark", null);

		assertThat(result, equalTo(mapModel));
	}

	@Test
	public void shouldReturnNullForUnknownTag() {
		Object result = bookmarksBuilder.createElement(mapModel, "unknown", null);

		assertThat(result, nullValue());
	}

	@Test
	public void shouldAddBookmarkOnEndElementWithValidNode() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("bookmark");
		dom.setAttribute("nodeId", "validNode");
		dom.setAttribute("name", "Test Bookmark");
		dom.setAttribute("opensAsRoot", "true");

		bookmarksBuilder.endElement(mapModel, "bookmark", mapModel, dom);

		assertThat(bookmarks.contains("validNode"), equalTo(true));
	}

	@Test
	public void shouldNotAddBookmarkForInvalidNode() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("bookmark");
		dom.setAttribute("nodeId", "invalidNode");
		dom.setAttribute("name", "Test Bookmark");
		dom.setAttribute("opensAsRoot", "false");

		bookmarksBuilder.endElement(mapModel, "bookmark", mapModel, dom);

		assertThat(bookmarks.contains("invalidNode"), equalTo(false));
	}

	@Test
	public void shouldNotAddBookmarkWithMissingNodeId() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("bookmark");
		dom.setAttribute("name", "Test Bookmark");
		dom.setAttribute("opensAsRoot", "false");

		bookmarksBuilder.endElement(mapModel, "bookmark", mapModel, dom);

		assertThat(bookmarks.size(), equalTo(1));
	}

	@Test
	public void shouldNotAddBookmarkWithMissingName() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("bookmark");
		dom.setAttribute("nodeId", "validNode");
		dom.setAttribute("opensAsRoot", "false");

		bookmarksBuilder.endElement(mapModel, "bookmark", mapModel, dom);

		assertThat(bookmarks.contains("validNode"), equalTo(false));
	}

	@Test
	public void shouldIgnoreNonMapModelParentInEndElement() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("bookmark");
		dom.setAttribute("nodeId", "validNode");
		dom.setAttribute("name", "Test Bookmark");

		bookmarksBuilder.endElement("notAMapModel", "bookmark", mapModel, dom);

		assertThat(bookmarks.contains("validNode"), equalTo(false));
	}

	@Test
	public void shouldIgnoreNonBookmarkTagInEndElement() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("othertag");
		dom.setAttribute("nodeId", "validNode");
		dom.setAttribute("name", "Test Bookmark");

		bookmarksBuilder.endElement(mapModel, "othertag", mapModel, dom);

		assertThat(bookmarks.contains("validNode"), equalTo(false));
	}

	@Test
	public void shouldDefaultOpensAsRootToFalse() {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		when(mapModel.getExtension(MapBookmarks.class)).thenReturn(bookmarks);

		XMLElement dom = new XMLElement("bookmark");
		dom.setAttribute("nodeId", "validNode");
		dom.setAttribute("name", "Test Bookmark");

		bookmarksBuilder.endElement(mapModel, "bookmark", mapModel, dom);

		NodeBookmark bookmark = bookmarks.getBookmark("validNode");
		assertThat(bookmark.getDescriptor().opensAsRoot(), equalTo(false));
	}

	@Test
	public void shouldWriteContentOnlyForValidNodes() throws IOException {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);
		bookmarksBuilder.writeContent(writer, mapModel, bookmarks);

		MapBookmarks actualBookmarks = MapBookmarks.of(mapModel);
		assertThat(actualBookmarks.size(), equalTo(1));
	}

	@Test
	public void shouldNotWriteContentForNonMapModel() throws IOException {
		MapBookmarks bookmarks = MapBookmarks.of(mapModel);

		bookmarksBuilder.writeContent(writer, "notAMapModel", bookmarks);
	}
}
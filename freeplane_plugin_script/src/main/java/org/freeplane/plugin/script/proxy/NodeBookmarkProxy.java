package org.freeplane.plugin.script.proxy;

import org.freeplane.api.BookmarkType;
import org.freeplane.api.Node;
import org.freeplane.api.NodeBookmark;
import org.freeplane.plugin.script.ScriptContext;

class NodeBookmarkProxy implements NodeBookmark {
	private final org.freeplane.features.bookmarks.mindmapmode.NodeBookmark coreBookmark;
	private final ScriptContext scriptContext;

	public NodeBookmarkProxy(org.freeplane.features.bookmarks.mindmapmode.NodeBookmark coreBookmark, ScriptContext scriptContext) {
		this.coreBookmark = coreBookmark;
		this.scriptContext = scriptContext;
	}

	@Override
	public Node getNode() {
		return new NodeProxy(coreBookmark.getNode(), scriptContext);
	}

	@Override
	public String getName() {
		return coreBookmark.getDescriptor().getName();
	}

	@Override
	public BookmarkType getBookmarkType() {
		return coreBookmark.getDescriptor().opensAsRoot() ? BookmarkType.ROOT : BookmarkType.SELECT;
	}
	
	@Override
	public void open() {
		coreBookmark.open();
	}
	
	@Override
	public void open(BookmarkType mode) {
		boolean openAsRoot = (mode == BookmarkType.ROOT);
		coreBookmark.open(openAsRoot);
	}
} 
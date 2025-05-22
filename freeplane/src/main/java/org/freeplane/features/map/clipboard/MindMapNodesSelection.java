/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.map.clipboard;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.util.Collection;
import java.util.stream.Stream;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;

public class MindMapNodesSelection implements Transferable, ClipboardOwner {
	public static final DataFlavor dropCopyActionFlavor;
	public static final DataFlavor dropLinkActionFlavor;
	public static final DataFlavor fileListFlavor;
	public static final DataFlavor htmlFlavor;
	public static final DataFlavor mindMapNodesFlavor;
	public static final DataFlavor mindMapNodeObjectsFlavor;
	public static final DataFlavor mindMapNodeSingleObjectsFlavor;
	static {
		DataFlavor mindMapNodesFlavorInit = null;
		DataFlavor mindMapNodeObjectsFlavorInit = null;
		DataFlavor mindMapNodeSingleObjectsFlavorInit = null;
		DataFlavor htmlFlavorInit = null;
		DataFlavor fileListFlavorInit = null;
		DataFlavor dropCopyActionFlavorInit = null;
		DataFlavor dropLinkActionFlavorInit = null;
		try {
			mindMapNodesFlavorInit = new DataFlavor("text/freeplane-nodes; class=java.lang.String");
			mindMapNodeObjectsFlavorInit = new DataFlavor("application/freeplane-nodes; class=java.util.Collection");
			mindMapNodeSingleObjectsFlavorInit = new DataFlavor("application/freeplane-single-nodes; class=java.util.Collection");
			htmlFlavorInit = new DataFlavor("text/html; class=java.lang.String");
			fileListFlavorInit = new DataFlavor("application/x-java-file-list; class=java.util.List");
			dropCopyActionFlavorInit = new DataFlavor("application/freeplane-drop-copy-action; class=java.lang.Integer");
			dropLinkActionFlavorInit = new DataFlavor("application/freeplane-drop-link-action; class=java.lang.Integer");
		}
		catch (final Exception e) {
			LogUtils.severe(e);
		}
		mindMapNodesFlavor = mindMapNodesFlavorInit;
		mindMapNodeObjectsFlavor = mindMapNodeObjectsFlavorInit;
		mindMapNodeSingleObjectsFlavor = mindMapNodeSingleObjectsFlavorInit;
		htmlFlavor = htmlFlavorInit;
		fileListFlavor = fileListFlavorInit;
		dropCopyActionFlavor = dropCopyActionFlavorInit;
		dropLinkActionFlavor = dropLinkActionFlavorInit;
	}
	final private String htmlContent;
	final private String nodesContent;
	final private String stringContent;
	private int dropActionContent;
	private Collection<NodeModel> nodes;
	private boolean selectionContainsSingleNodes;

	public MindMapNodesSelection(final String nodesContent, final String stringContent,
	                             final String htmlContent) {
		this.nodesContent = nodesContent;
		this.stringContent = stringContent;
		this.htmlContent = htmlContent;
		this.dropActionContent = DnDConstants.ACTION_MOVE;
	}

	public MindMapNodesSelection(final String nodesContent) {
	    this(nodesContent, null, null);
    }

	@Override
	public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(DataFlavor.stringFlavor)) {
			return stringContent;
		}
		if (flavor.equals(MindMapNodesSelection.mindMapNodesFlavor)) {
			return nodesContent;
		}
		if (flavor.equals(MindMapNodesSelection.dropCopyActionFlavor)
				|| flavor.equals(MindMapNodesSelection.dropLinkActionFlavor)) {
			return dropActionContent;
		}
		if (flavor.equals(MindMapNodesSelection.htmlFlavor) && htmlContent != null) {
			return htmlContent;
		}
		if (containsObjectsFor(flavor)) {
			return nodes;
		}
		throw new UnsupportedFlavorException(flavor);
	}

	boolean containsObjectsFor(final DataFlavor flavor) {
		return nodes != null && (flavor.equals(MindMapNodesSelection.mindMapNodeObjectsFlavor) && ! selectionContainsSingleNodes
				|| flavor.equals(MindMapNodesSelection.mindMapNodeSingleObjectsFlavor) && selectionContainsSingleNodes);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return Stream.of(DataFlavor.stringFlavor, MindMapNodesSelection.mindMapNodesFlavor,
		        MindMapNodesSelection.htmlFlavor,
		        MindMapNodesSelection.dropCopyActionFlavor, MindMapNodesSelection.dropLinkActionFlavor,
		        MindMapNodesSelection.mindMapNodeObjectsFlavor , MindMapNodesSelection.mindMapNodeSingleObjectsFlavor)
				.filter(this::isDataFlavorSupported)
				.toArray(DataFlavor[]::new);
	}

	@Override
	public boolean isDataFlavorSupported(final DataFlavor flavor) {
		if (flavor.equals(DataFlavor.stringFlavor) && stringContent != null) {
			return true;
		}
		if (flavor.equals(MindMapNodesSelection.mindMapNodesFlavor) && nodesContent != null) {
			return true;
		}
		if (flavor.equals(MindMapNodesSelection.dropCopyActionFlavor) && dropActionContent == DnDConstants.ACTION_COPY) {
			return true;
		}
		if (flavor.equals(MindMapNodesSelection.dropLinkActionFlavor) && dropActionContent == DnDConstants.ACTION_LINK) {
			return true;
		}
		if (flavor.equals(MindMapNodesSelection.htmlFlavor) && htmlContent != null) {
			return true;
		}
		if (containsObjectsFor(flavor)) {
			return true;
		}
		return false;
	}

	@Override
	public void lostOwnership(final Clipboard clipboard, final Transferable contents) {
	}

	public void setDropAction(final Integer dropActionContent) {
		this.dropActionContent = dropActionContent;
	}

	public void setNodeObjects(Collection<NodeModel> collection, boolean selectionContainsSingleNodes) {
	    nodes = collection;
	    this.selectionContainsSingleNodes = selectionContainsSingleNodes;
    }
}

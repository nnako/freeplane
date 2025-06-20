/**
 *
 */
package org.freeplane.plugin.script.proxy;

import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.NumberMath;
import org.freeplane.api.Attributes;
import org.freeplane.api.BookmarkType;
import org.freeplane.api.ChildNodesLayout;
import org.freeplane.api.Cloud;
import org.freeplane.api.ConditionalStyles;
import org.freeplane.api.Connector;
import org.freeplane.api.DependencyLookup;
import org.freeplane.api.LayoutOrientation;
import org.freeplane.api.LengthUnit;
import org.freeplane.api.Node;
import org.freeplane.api.NodeBookmark;
import org.freeplane.api.NodeCondition;
import org.freeplane.api.NodeGeometry;
import org.freeplane.api.NodeRO;
import org.freeplane.api.NodeStyle;
import org.freeplane.api.NodeToComparableMapper;
import org.freeplane.api.Quantity;
import org.freeplane.api.Reminder;
import org.freeplane.api.Side;
import org.freeplane.api.Tags;
import org.freeplane.core.undo.IActor;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.MapBookmarks;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmarkDescriptor;
import org.freeplane.features.encrypt.Base64Coding;
import org.freeplane.features.encrypt.PasswordStrategy;
import org.freeplane.features.explorer.AccessedNodes;
import org.freeplane.features.explorer.MapExplorer;
import org.freeplane.features.explorer.NodeNotFoundException;
import org.freeplane.features.explorer.mindmapmode.MMapExplorerController;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.filter.condition.ICondition;
import org.freeplane.features.format.IFormattedObject;
import org.freeplane.features.layout.mindmapmode.MLayoutController;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.EncryptionModel;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapController.Direction;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.MapNavigationUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.clipboard.MapClipboardController.CopiedNodeSet;
import org.freeplane.features.map.clipboard.MindMapPlainTextWriter;
import org.freeplane.features.map.mindmapmode.InsertionRelation;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.clipboard.MMapClipboardController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.nodelocation.mindmapmode.MLocationController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.ui.ViewController;
import org.freeplane.plugin.script.ScriptContext;
import org.freeplane.plugin.script.dependencies.DependencySearchStrategy;

import groovy.lang.Closure;

class NodeProxy extends AbstractProxy<NodeModel> implements Proxy.Node {
	private static final Integer ONE = 1;
	private static final Integer ZERO = 0;

	public NodeProxy(final NodeModel node, final ScriptContext scriptContext) {
		super(node, scriptContext);
		reportOwnedNodeAccess();
	}

	// Node: R/W
	@Override
	public Proxy.Connector addConnectorTo(final Node target) {
		return addConnectorTo(target.getId());
	}

	// Node: R/W
	@Override
	public Proxy.Connector addConnectorTo(final String targetNodeID) {
		final MLinkController linkController = (MLinkController) getLinkController();
		final ConnectorModel connectorModel = linkController.addConnector(getDelegate(), targetNodeID);
		return new ConnectorProxy(connectorModel, getScriptContext());
	}

    private LinkController getLinkController() {
        return LinkController.getController(getModeController());
    }

	// Node: R/W
	@Override
	public Node createChild() {
	    return createChild("");
	}

    private NodeModel extracted(NodeModel target, final Object value) {
        return new NodeModel(value, target.getMap());
    }

	private MMapController getMapController() {
		return (MMapController) getModeController().getMapController();
	}

	// Node: R/W
	@Override
	public Node createChild(final Object value) {
        return createChild(getDelegate(), value, getDelegate().getChildCount());
	}

    private Node createChild(NodeModel parent, final Object value, int position) {
        final NodeModel newNodeModel = extracted(parent, value);
        if(parent.isRoot())
            newNodeModel.setSide( MapController.suggestNewChildSide(parent, NodeModel.Side.DEFAULT));
        getMapController().insertNode(newNodeModel, parent, position);
        return new NodeProxy(newNodeModel, getScriptContext());
    }

	// Node: R/W
	@Override
	public Node createChild(final int position) {
        return createChild(getDelegate(), "", position);
	}

	// Node: R/W
	@Override
	public Node appendChild(final NodeRO node) {
		return appendBranchImpl(node, false);
	}

	// Node: R/W
	@Override
	public Node appendBranch(final NodeRO node) {
		return appendBranchImpl(node, true);
	}

	private Node appendBranchImpl(final NodeRO node, final boolean withChildren) {
	    final MMapClipboardController clipboardController = (MMapClipboardController) getClipboardController();
		NodeModel source = ((NodeProxy) node).getDelegate();
		NodeModel target = getDelegate();
        final NodeModel newNodeModel = clipboardController.duplicate(source, target.getMap(), withChildren);
        newNodeModel.setSide(MapController.suggestNewChildSide(target, NodeModel.Side.DEFAULT));
        final NodeModel parent = target;
        getMapController().insertNode(newNodeModel, parent, parent.getChildCount());
		return new NodeProxy(newNodeModel, getScriptContext());
    }

	// Node: R/W
	@Override
	public void delete() {
		getMapController().deleteNode(getDelegate());
	}

	// NodeRO: R
	@Override
	public Proxy.Attributes getAttributes() {
		return AttributesProxy.withRawValues(getDelegate(), getScriptContext());
	}

	// NodeRO: R
	@Override
	public Convertible getAt(final String attributeName) {
		final Object value = getAttributes().getFirst(attributeName);
		return ProxyUtils.attributeValueToConvertible(getDelegate(), getScriptContext(), value);
	}

	// Node: R/W
	@Override
	public Object putAt(final String attributeName, final Object value) {
		final Attributes attributes = getAttributes();
		if (value == null) {
			final int index = attributes.findFirst(attributeName);
			if (index != -1)
				attributes.remove(index);
			// else: ignore request
		}
		else {
			attributes.set(attributeName, value);
		}
		return value;
	}

	// Node: R/W
	@Override
	public void setAttributes(final Map<String, Object> attributeMap) {
		final Attributes attributes = getAttributes();
		attributes.clear();
		for (final Entry<String, Object> entry : attributeMap.entrySet()) {
			attributes.set(entry.getKey(), entry.getValue());
		}
	}

	// Node: R/W
	@Override
	public void setDetails(final Object details) {
		setDetailsText(Convertible.toString(details));
	}

	// Node: R/W
    @Override
	public void setDetailsText(final String text) {
        final MTextController textController = (MTextController) getTextController();
		NodeModel delegate = getDelegate();
        if (text == null || text.isEmpty()) {
			textController.setDetailsHidden(delegate, false);
			textController.setDetails(delegate, null);
		}
		else{
	        String detailsContentType = textController.getDetailsContentType(delegate);
			textController.setDetails(delegate,
			        ! HtmlUtils.isHtml(text) && TextController.isHtmlContentType(detailsContentType)
                    ?  HtmlUtils.textToHTML(text) : text);
		}
    }

	// Node: R/W
	@Override
	public void setHideDetails(final boolean hide) {
		final MTextController controller = (MTextController) getTextController();
		controller.setDetailsHidden(getDelegate(), hide);
    }

	// NodeRO: R
	@Override
	public int getChildPosition(final Node childNode) {
		final NodeModel childNodeModel = ((NodeProxy) childNode).getDelegate();
		return getDelegate().getIndex(childNodeModel);
	}

	// NodeRO: R
	@Override
	public List<Proxy.Node> getChildren() {
		return ProxyUtils.createListOfChildren(getDelegate(), getScriptContext());
	}

    // NodeRO: R
    @Override
	public Cloud getCloud() {
        return new CloudProxy(this);
    }

	// NodeRO: R
	@Override
	public Collection<Proxy.Connector> getConnectorsIn() {
		return new ConnectorInListProxy(this);
	}

	// NodeRO: R
	@Override
	public Collection<Proxy.Connector> getConnectorsOut() {
		return new ConnectorOutListProxy(this);
	}

	// NodeRO: R
	@Override
	public Convertible getDetails() {
		final String detailsText = DetailModel.getDetailText(getDelegate());
		return (detailsText == null) ? null : new ConvertibleHtmlText(getDelegate(), getScriptContext(), detailsText);
	}

	// NodeRO: R
	@Override
	public String getDetailsText() {
		return DetailModel.getDetailText(getDelegate());
	}

	@Override
    public String getDetailsContentType() {
		final NodeModel nodeModel = getDelegate();
		final String contentType = getTextController().getDetailsContentType(nodeModel);
		return contentType;
	}

	@Override
    public void setDetailsContentType(String contentType) {
		MTextController textController = (MTextController) getTextController();
		if(contentType != null
				&& ! Stream.of(textController.getDetailContentTypes()).anyMatch(contentType::equals)) {
			throw new IllegalArgumentException("Unknown content type " + contentType);
		}
		final NodeModel nodeModel = getDelegate();
		textController.setDetailsContentType(nodeModel, contentType);
	}

	// NodeRO: R
	@Override
	public boolean getHideDetails() {
		final DetailModel detailText = DetailModel.getDetail(getDelegate());
		return detailText != null && detailText.isHidden();
    }

	// NodeRO: R
	@Override
	public Proxy.ExternalObject getExternalObject() {
		return new ExternalObjectProxy(getDelegate(), getScriptContext());
	}

	// NodeRO: R
	@Override
	public Proxy.Icons getIcons() {
		return new IconsProxy(getDelegate(), getScriptContext());
	}

	// NodeRO: R
	@Override
	public Proxy.Link getLink() {
		return new LinkProxy(getDelegate(), getScriptContext());
	}

	// NodeRO: R
    @Override
	public Reminder getReminder() {
        return new ReminderProxy(getDelegate(), getScriptContext());
    }

	// NodeRO: R
	@Override
	public String getId() {
		return getDelegate().createID();
	}

	// NodeRO: R
	@Override
	@Deprecated
	public String getNodeID() {
		return getId();
	}

	// NodeRO: R
	@Override
	public int getNodeLevel(final boolean countHidden) {
		NodeModel node = getDelegate();
		return countHidden ? node.getNodeLevel() : node.getNodeLevel(FilterController.getFilter(node.getMap()));
	}

	// NodeRO: R
	public String getPlainNote() {
		final String noteText = NoteModel.getNoteText(getDelegate());
		return noteText == null ? null : HtmlUtils.htmlToPlain(noteText);
	}

	// NodeRO: R
	@Override
	public String getNoteText() {
		return NoteModel.getNoteText(getDelegate());
	}

	// NodeRO: R
	@Override
	public Convertible getNote() {
		final String noteText = getNoteText();
		return (noteText == null) ? null : new ConvertibleNoteText(getDelegate(), getScriptContext(), noteText);
	}

	@Override
    public String getNoteContentType() {
		final NodeModel nodeModel = getDelegate();
		final String contentType = getNoteController().getNoteContentType(nodeModel);
		return contentType;
	}

	@Override
    public void setNoteContentType(String contentType) {
		MNoteController noteController = (MNoteController) getNoteController();
		if(contentType != null
				&& ! Stream.of(noteController.getNoteContentTypes()).anyMatch(contentType::equals)) {
			throw new IllegalArgumentException("Unknown content type " + contentType);
		}
		final NodeModel nodeModel = getDelegate();
		noteController.setNoteContentType(nodeModel, contentType);
	}

	// NodeRO: R
	@Override
	public Node getParent() {
		final NodeModel parentNode = getDelegate().getParentNode();
		return parentNode != null ? new NodeProxy(parentNode, getScriptContext()) : null;
	}

	// NodeRO: R
	@Override
	@Deprecated
	public Node getParentNode() {
		return getParent();
	}

    // NodeRO: R
    @Override
	public List<? extends Node> getPathToRoot() {
        return ProxyUtils.createNodeList(Arrays.asList(getDelegate().getPathToRoot()), getScriptContext());
    }

    // NodeRO: R
    @Override
	public Node getNext() {
        final NodeModel node = MapNavigationUtils.findNext(Direction.FORWARD, getDelegate(), null);
        return node == null ? null : new NodeProxy(node, getScriptContext());
    }

    // NodeRO: R
    @Override
	public Node getPrevious() {
        final NodeModel node = MapNavigationUtils.findPrevious(Direction.BACK, getDelegate(), null);
        return node == null ? null : new NodeProxy(node, getScriptContext());
    }

	// NodeRO: R
	@Override
	public String getPlainText() {
		final NodeModel node = getDelegateForValueAccess();
		return HtmlUtils.htmlToPlain(node.getText());
	}

	private NodeModel getDelegateForValueAccess() {
		reportValueAccess();
		return getDelegate();
	}

	private void reportValueAccess() {
		final ScriptContext scriptContext = getScriptContext();
		if (scriptContext != null)
			scriptContext.accessValue(getDelegate());
	}

	// NodeRO: R
	@Override
	@Deprecated
	public String getPlainTextContent() {
		return getPlainText();
	}

	// NodeRO: R
	@Override
	public String getHtmlText() {
		final NodeModel node = getDelegateForValueAccess();
		final String nodeText = node.getText();
		if (HtmlUtils.isHtml(nodeText))
			return nodeText;
		else
			return HtmlUtils.plainToHTML(nodeText);
	}

	// NodeRO: R
	@Override
	public NodeStyle getStyle() {
		return new NodeStyleProxy(getDelegate(), getScriptContext());
	}

	// NodeRO: R
	@Override
	public boolean hasStyle(final String styleName) {
		return NodeStyleProxy.hasStyle(getDelegate(), styleName);
	}

	@Override
	public ConditionalStyles getConditionalStyles() {
		return new NodeConditionalStylesProxy(getDelegate(), getScriptContext());
	}

	// NodeRO: R
	@Override
	public String getText() {
		return getDelegateForValueAccess().getText();
	}

	// NodeRO: R
	@Override
	public String getTransformedText() {
		final TextController textController = getTextController();
		return textController.getTransformedTextNoThrow(getDelegateForValueAccess());
	}

	// NodeRO: R
	@Override
	public String getShortText() {
		final TextController textController = getTextController();
		return textController.getShortPlainText(getDelegateForValueAccess());
	}

	// NodeRO: R
	@Override
	public String getDisplayedText(){
		if(isMinimized())
			return getShortText();
		else
			return getTransformedText();
	}

	// NodeRO: R
	@Override
	public boolean isMinimized(){
		final TextController textController = getTextController();
		return textController.isMinimized(getDelegate());
	}

	// NodeRO: R
	@Override
	public Object getObject() {
		final NodeModel node = getDelegateForValueAccess();
		final Object userObject = node.getUserObject();
		if (userObject instanceof IFormattedObject)
			return ((IFormattedObject) userObject).getObject();
		return userObject;
	}

	// NodeRO: R
	@Override
	public byte[] getBinary() {
		final NodeModel node = getDelegateForValueAccess();
		return Base64Coding.decode64(node.getText().replaceAll("\\s", ""));
	}

	// NodeRO: R
	@Override
	public String getFormat() {
		final NodeModel nodeModel = getDelegate();
		final String format = getTextController().getNodeFormat(nodeModel);
		if (format == null && nodeModel.getUserObject() instanceof IFormattedObject)
			return ((IFormattedObject) nodeModel.getUserObject()).getPattern();
		return format;
	}

	// NodeRO: R
	@Override
	public Convertible getTo() {
		final NodeModel node = getDelegateForValueAccess();
		return ProxyUtils.nodeModelToConvertible(node, getScriptContext());
	}

	// NodeRO: R
	@Override
	public Convertible getValue() {
		return getTo();
	}

	// NodeRO: R
	@Override
	public boolean isDescendantOf(final Node otherNode) {
		// no need to trace this since it's already logged
		final NodeModel otherNodeModel = ((NodeProxy) otherNode).getDelegate();
		NodeModel node = getDelegate();
		do {
			if (node.equals(otherNodeModel)) {
				return true;
			}
			node = node.getParentNode();
		} while (node != null);
		return false;
	}

	// NodeRO: R
	@Override
	public boolean isFolded() {
		IMapSelection selection = Controller.getCurrentController().getSelection();
		NodeModel node = getDelegate();
		return selection != null ? selection.isFolded(node) : node.isFolded();
	}

    // NodeRO: R
    @Override
	public boolean isFree() {
        final FreeNode freeNode = getFreeNodeHook();
        return freeNode.isActive(getDelegate());
    }

	// NodeRO: R
	@Override
	public boolean isLeaf() {
		return getDelegate().isLeaf();
	}

	// NodeRO: R
	@Override
	public boolean isTopOrLeft() {
		NodeModel node = getDelegate();
		return node.isTopOrLeft(node.getMap().getRootNode());
	}

	@Override
	public Side getSideAtRoot() {
		return Side.values()[getDelegate().getSide().ordinal()];
	}

	// NodeRO: R
	@Override
	public boolean isTopOrLeftOnViewsWithRoot(NodeRO viewRoot) {
		if(! (viewRoot instanceof NodeProxy))
			return false;
        NodeModel node = getDelegate();
		NodeModel root = ((NodeProxy)viewRoot).getDelegate();
		return node.isTopOrLeft(root);
	}

	// NodeRO: R
	@Override
	public boolean isRoot() {
		return getDelegate().isRoot();
	}

	// NodeRO: R
	@Override
	public boolean isVisible() {
        NodeModel node = getDelegate();
		return getDelegate().hasVisibleContent(FilterController.getFilter(node.getMap()));
	}

	// NodeRO: R
	@Override
	public boolean isVisibleOnViewsWithRoot(NodeRO viewRoot) {
		if(! (viewRoot instanceof NodeProxy))
			return false;
        NodeModel node = getDelegate();
		NodeModel root = ((NodeProxy)viewRoot).getDelegate();
		return node == root || root.getMap() == node.getMap() && node.isDescendantOf(root) && isVisible();
	}

	// Node: R/W
	@Override
	public void moveTo(final Node parentNodeProxy) {
		final NodeModel parentNode = ((NodeProxy) parentNodeProxy).getDelegate();
        final NodeModel movedNode = getDelegate();
        getMapController().moveNodes(Arrays.asList(movedNode), parentNode, InsertionRelation.AS_CHILD);
	}

	// Node: R/W
	@Override
	public void moveTo(final Node parentNodeProxy, final int position) {
        final NodeModel parentNode = ((NodeProxy) parentNodeProxy).getDelegate();
        final NodeModel movedNode = getDelegate();
		getFreeNodeHook().undoableDeactivateHook(movedNode);
		getMapController().moveNodes(Arrays.asList(movedNode), parentNode, position);
	}

	// Node: R/W
	@Override
	public void removeConnector(final Connector connectorToBeRemoved) {
		final ConnectorProxy connectorProxy = (ConnectorProxy) connectorToBeRemoved;
		final ConnectorModel link = connectorProxy.getConnector();
		final MLinkController linkController = (MLinkController) getLinkController();
		linkController.removeArrowLink(link);
	}

	// Node: R/W
	@Override
	public void setFolded(final boolean folded) {
	    NodeModel node = getDelegate();
        getMapController().setFolded(node, folded, FilterController.getFilter(node.getMap()));
	}

    // Node: R/W
    @Override
	public void setFree(final boolean free) {
        final FreeNode freeNode = getFreeNodeHook();
        if (free != freeNode.isActive(getDelegate()))
            freeNode.undoableToggleHook(getDelegate());
    }

	// Node: R/W
	@Override
	public void setMinimized(final boolean shortened) {
		final MTextController textController = (MTextController) getTextController();
		textController.setIsMinimized(getDelegate(), shortened);
	}

	// Node: R/W
	@Override
	public void setNote(final Object value) {
		setNoteText(Convertible.toString(value));
	}

	// Node: R/W
	@Override
	public void setNoteText(final String text) {
	    NodeModel delegate = getDelegate();
		final MNoteController noteController = (MNoteController) getNoteController();
		String noteContentType = noteController.getNoteContentType(delegate);
        noteController.setNoteText(delegate,
                ! HtmlUtils.isHtml(text) && TextController.isHtmlContentType(noteContentType)
                ?  HtmlUtils.textToHTML(text) : text);
	}

	// Node: R/W
	@Override
	public void setText(final Object value) {
		if (value instanceof String) {
			final MTextController textController = (MTextController) getTextController();
			textController.setNodeText(getDelegate(), (String) value);
		}
		else {
			setObject(value);
		}
	}

	// Node: R/W
	@Override
	public void setObject(final Object object) {
		final MTextController textController = (MTextController) getTextController();
		textController.setNodeObject(getDelegate(), ProxyUtils.transformObject(object, null));
	}

	// Node: R/W
	@Override
	public void setDateTime(final Date date) {
		final MTextController textController = (MTextController) getTextController();
		textController.setNodeObject(getDelegate(), ProxyUtils.createDefaultFormattedDateTime(date));
	}

	// Node: R/W
	@Override
	public void setBinary(final byte[] data) {
		setObject(Base64Coding.encode64(data).replaceAll("(.{74})", "$1\n"));
	}

	@Override
	public void setFormat(final String format) {
		final MNodeStyleController styleController = (MNodeStyleController) getStyleController();
		styleController.setNodeFormat(getDelegate(), format);
	}

    private NodeStyleController getStyleController() {
        return getModeController().getExtension(NodeStyleController.class);
    }

	@Deprecated
    @Override
	public void setLeft(final boolean isTopOrLeft) {
		setSide(isTopOrLeft ? NodeModel.Side.TOP_OR_LEFT : NodeModel.Side.BOTTOM_OR_RIGHT);
	}

	@Override
	public void setSideAtRoot(Side side) {
		setSide(NodeModel.Side.values()[side.ordinal()]);
	}

	private void setSide(NodeModel.Side side) {
		getMapController().setSide(Collections.singletonList(getDelegate()), side);
	}

	// NodeRO: R
	@Override
	public Proxy.Map getMindMap() {
		final MapModel map = getDelegate().getMap();
		return map != null ? new MapProxy(map, getScriptContext()) : null;
	}

	// NodeRO: R
	@Override
	@Deprecated
	public List<? extends Node> find(final ICondition condition) {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.find(condition, delegate, getScriptContext());
	}

    private void reportBranchAccess(final NodeModel delegate) {
        ScriptContext scriptContext = getScriptContext();
        if (scriptContext != null)
            scriptContext.accessBranch(delegate);
    }

    private void reportCloneAccess() {
        ScriptContext scriptContext = getScriptContext();
        if (scriptContext != null)
            scriptContext.accessClones(getDelegate());
    }

	// NodeRO: R
	@Override
	public List<? extends Node> find(final Closure<Boolean> closure) {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.find(closure, delegate, getScriptContext());
	}

	// NodeRO: R
	@Override
	public List<? extends Node> find(boolean withAncestors, boolean withDescendants, final Closure<Boolean> closure) {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.find(withAncestors, withDescendants, closure, delegate, getScriptContext());
	}

	@Override
	public List<? extends Node> find(final NodeCondition condition) {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.find(condition, delegate, getScriptContext());
	}

	@Override
	public List<? extends Node> find(boolean withAncestors, boolean withDescendants, final NodeCondition condition) {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.find(withAncestors, withDescendants, condition, delegate, getScriptContext());
	}

	// NodeRO: R
	@Override
	public List<? extends Node> findAll() {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.findAll(delegate, getScriptContext(), false);
    }

	// NodeRO: R
	@Override
	public List<? extends Node> findAllDepthFirst() {
		final NodeModel delegate = getDelegate();
		reportBranchAccess(delegate);
		return ProxyUtils.findAll(delegate, getScriptContext(), true);
    }

	// NodeRO: R
	@Override
	public Date getLastModifiedAt() {
		return getDelegate().getHistoryInformation().getLastModifiedAt();
	}

	// Node: R/W
	@Override
	public void setLastModifiedAt(final Date date) {
		final Date oldDate = getDelegate().getHistoryInformation().getLastModifiedAt();
		final IActor actor = new IActor() {
			@Override
			public void act() {
				getDelegate().getHistoryInformation().setLastModifiedAt(date);
			}

			@Override
			public String getDescription() {
				return "setLastModifiedAt";
			}

			@Override
			public void undo() {
				getDelegate().getHistoryInformation().setLastModifiedAt(oldDate);
			}
		};
		getModeController().execute(actor, getDelegate().getMap());
	}

	// NodeRO: R
	@Override
	public Date getCreatedAt() {
		return getDelegate().getHistoryInformation().getCreatedAt();
	}

	// Node: R/W
	@Override
	public void setCreatedAt(final Date date) {
		final Date oldDate = getDelegate().getHistoryInformation().getCreatedAt();
		final IActor actor = new IActor() {
			@Override
			public void act() {
				getDelegate().getHistoryInformation().setCreatedAt(date);
			}

			@Override
			public String getDescription() {
				return "setCreatedAt";
			}

			@Override
			public void undo() {
				getDelegate().getHistoryInformation().setCreatedAt(oldDate);
			}
		};
		getModeController().execute(actor, getDelegate().getMap());
	}

	//
	// Node arithmetics for
	//     Node <operator> Number
	//     Node <operator> Node
	// See NodeArithmeticsCategory for
	//     Number <operator> Node
	//
	public Number and(final Number number) {
		return NumberMath.and(getTo().getNum0(), number);
	}

	public Number and(final Node node) {
		return NumberMath.and(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number div(final Number number) {
		return NumberMath.divide(getTo().getNum0(), number);
	}

	public Number div(final Node node) {
		return NumberMath.divide(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number minus(final Number number) {
		return NumberMath.subtract(getTo().getNum0(), number);
	}

	public Number minus(final Node node) {
		return NumberMath.subtract(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number mod(final Number number) {
		return NumberMath.mod(getTo().getNum0(), number);
	}

	public Number mod(final Node node) {
		return NumberMath.mod(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number multiply(final Number number) {
		return NumberMath.multiply(getTo().getNum0(), number);
	}

	public Number multiply(final Node node) {
		return NumberMath.multiply(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number or(final Number number) {
		return NumberMath.or(getTo().getNum0(), number);
	}

	public Number or(final Node node) {
		return NumberMath.or(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number plus(final Number number) {
		return NumberMath.add(getTo().getNum0(), number);
	}

	public Number plus(final Node node) {
		return NumberMath.add(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number power(final Number number) {
		return DefaultGroovyMethods.power(getTo().getNum0(), number);
	}

	public Number power(final Node node) {
		return DefaultGroovyMethods.power(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number xor(final Number number) {
		return NumberMath.xor(getTo().getNum0(), number);
	}

	public Number xor(final Node node) {
		return NumberMath.xor(getTo().getNum0(), node.getTo().getNum0());
	}

	public Number negative() {
		return NumberMath.subtract(ZERO, getTo().getNum0());
	}

	public Number next() {
		return NumberMath.add(getTo().getNum0(), ONE);
	}

	public Number positive() {
		return getTo().getNum0();
	}

	public Number previous() {
		return NumberMath.subtract(getTo().getNum0(), ONE);
	}

    @Override
	public boolean hasEncryption() {
        return getEncryptionModel() != null;
    }

    @Override
	public boolean isEncrypted() {
        final EncryptionModel encryptionModel = getEncryptionModel();
        return encryptionModel != null && !encryptionModel.isAccessible();
    }

    private EncryptionModel getEncryptionModel() {
        return EncryptionModel.getModel(getDelegate());
    }

    @Override
	public void encrypt(final String password) {
        if (!isEncrypted())
            getEncryptionController().toggleLock(getDelegate(), makePasswordStrategy(password));
    }

    @Override
	public void decrypt(final String password) {
        if (isEncrypted())
            getEncryptionController().toggleLock(getDelegate(), makePasswordStrategy(password));
    }

    @Override
	public void removeEncryption(final String password) {
        getEncryptionController().removeEncryption(getDelegate(), makePasswordStrategy(password));
    }

    private PasswordStrategy makePasswordStrategy(final String password) {
        return new PasswordStrategy() {
            @Override
			public StringBuilder getPassword(NodeModel node) {
                return new StringBuilder(password);
            }

            @Override
			public StringBuilder getPasswordWithConfirmation(NodeModel node) {
                return getPassword(node);
            }

            @Override
			public void onWrongPassword() {
                LogUtils.info("wrong password for node " + getDelegate());
                setStatusInfo(TextUtils.getText("accessories/plugins/EncryptNode.properties_wrong_password"));
            }

            @Override
			public boolean isCancelled() {
                return false;
            }
        };
    }

	private void setStatusInfo(final String text) {
        final ViewController viewController = Controller.getCurrentController().getViewController();
        viewController.out(text);
    }

	@Override
	public Quantity<LengthUnit> getHorizontalShiftAsLength(){
		return getLocationController().getHorizontalShift(getDelegate());
	}

	@Override
	public Quantity<LengthUnit> getVerticalShiftAsLength(){
		return getLocationController().getVerticalShift(getDelegate());
	}

	@Override
	public void setHorizontalShift(final int horizontalShift){
		final Quantity<LengthUnit> horizontalShiftQuantity = new Quantity<LengthUnit>(horizontalShift, LengthUnit.px);
		((MLocationController) getLocationController()).setHorizontalShift(getDelegate(),horizontalShiftQuantity);
	}

	@Override
	public void setHorizontalShift(final Quantity<LengthUnit> verticalShift) {
		((MLocationController) getLocationController()).setHorizontalShift(getDelegate(), verticalShift);
	}

	@Override
	public void setHorizontalShift(final String verticalShift) {
		((MLocationController) getLocationController()).setHorizontalShift(getDelegate(), Quantity.fromString(verticalShift, LengthUnit.px));
	}

	@Override
	public void setVerticalShift(final int verticalShift){
		final Quantity<LengthUnit> verticalShiftQuantity = new Quantity<LengthUnit>(verticalShift, LengthUnit.px);
		((MLocationController) getLocationController()).setVerticalShift(getDelegate(), verticalShiftQuantity);
	}

	@Override
	public void setVerticalShift(final Quantity<LengthUnit> verticalShift) {
		((MLocationController) getLocationController()).setVerticalShift(getDelegate(), verticalShift);
	}

	@Override
	public void setVerticalShift(final String verticalShift) {
		((MLocationController) getLocationController()).setVerticalShift(getDelegate(), Quantity.fromString(verticalShift, LengthUnit.px));
	}

    @Override
    public Quantity<LengthUnit> getMinimalDistanceBetweenChildrenAsLength(){
        return getLocationController().getCommonVGapBetweenChildren(getDelegate());
    }

    @Override
    public Quantity<LengthUnit> getBaseDistanceToChildrenAsLength(){
        return getLocationController().getBaseHGapToChildren(getDelegate());
    }



	@Override
	public void setMinimalDistanceBetweenChildren(final int minimalDistanceBetweenChildren){
		final Quantity<LengthUnit> minimalDistanceBetweenChildrenQuantity = new Quantity<LengthUnit>(minimalDistanceBetweenChildren, LengthUnit.px);
		((MLocationController) getLocationController()).setCommonVGapBetweenChildren(getDelegate(), minimalDistanceBetweenChildrenQuantity);
	}

    @Override
    public void setBaseDistanceToChildren(final int baseDistanceToChildren){
        final Quantity<LengthUnit> minimalDistanceBetweenChildrenQuantity = new Quantity<LengthUnit>(baseDistanceToChildren, LengthUnit.px);
        ((MLocationController) getLocationController()).setBaseHGapToChildren(getDelegate(), minimalDistanceBetweenChildrenQuantity);
    }

    @Override
    public void setChildNodesLayout(final ChildNodesLayout sides){
        ((MLayoutController) getLayoutController()).setChildNodesLayout(getDelegate(), sides);
    }
    @Override
    public ChildNodesLayout getChildNodesLayout() {
        return getLayoutController().getChildNodesLayout(getDelegate());
    }

    @Override
    public LayoutOrientation getLayoutOrientation() {
        return getLayoutController().getLayoutOrientation(getDelegate());
    }

	@Override
	public void setMinimalDistanceBetweenChildren(final Quantity<LengthUnit> minimalDistanceBetweenChildren) {
		((MLocationController) getLocationController()).setCommonVGapBetweenChildren(getDelegate(), minimalDistanceBetweenChildren);
	}

	@Override
	public void setMinimalDistanceBetweenChildren(final String minimalDistanceBetweenChildren) {
		((MLocationController) getLocationController()).setCommonVGapBetweenChildren(getDelegate(), Quantity.fromString(minimalDistanceBetweenChildren, LengthUnit.px));
	}

    @Override
    public void setBaseDistanceToChildren(final Quantity<LengthUnit> baseDistance){
        ((MLocationController) getLocationController()).setBaseHGapToChildren(getDelegate(), baseDistance);
    }

    @Override
    public void setBaseDistanceToChildren(final String baseDistance){
        final Quantity<LengthUnit> baseDistancerenQuantity = Quantity.fromString(baseDistance, LengthUnit.px);
        ((MLocationController) getLocationController()).setBaseHGapToChildren(getDelegate(), baseDistancerenQuantity);
    }

	@Override
	public void sortChildrenBy(final Closure<Comparable<Object>> closure) {
		final Comparator<NodeModel> comparator = comparatorByClosureResult(closure);
		sortChildrenBy(comparator);
	}

	@Override
	public void sortChildrenBy(final NodeToComparableMapper mapper) {
		final Comparator<NodeModel> comparator = comparatorByMapper(mapper);
		sortChildrenBy(comparator);
	}

	private Comparator<NodeModel> comparatorByMapper(final NodeToComparableMapper mapper) {
		return new Comparator<NodeModel>() {
			@Override
			public int compare(final NodeModel o1, final NodeModel o2) {
				final NodeProxy p1 = new NodeProxy(o1, getScriptContext());
				final NodeProxy p2 = new NodeProxy(o1, getScriptContext());
				return mapper.toComparable(p1).compareTo(mapper.toComparable(p2));
			}
		};
	}
	private void sortChildrenBy(final Comparator<NodeModel> comparator) {
		reportOwnedNodeAccess();
		final NodeModel node = getDelegate();
		final ArrayList<NodeModel> children = new ArrayList<NodeModel>(node.getChildren());
		Collections.sort(children, comparator);
		final MMapController mapController = (MMapController) getModeController().getMapController();
		int i = 0;
		for (final NodeModel child : children) {
			getFreeNodeHook()
			    .undoableDeactivateHook(child);
			mapController.moveNode(child, i++);
		}
	}

	private void reportOwnedNodeAccess() {
		final ScriptContext scriptContext = getScriptContext();
		if (scriptContext != null)
			scriptContext.accessNode(getDelegate());
	}


	private Comparator<NodeModel> comparatorByClosureResult(final Closure<Comparable<Object>> closure) {
		return new Comparator<NodeModel>() {
			@Override
			public int compare(final NodeModel o1, final NodeModel o2) {
				final NodeProxy p1 = new NodeProxy(o1, getScriptContext());
				final NodeProxy p2 = new NodeProxy(o2, getScriptContext());
				return closure.call(p1).compareTo(closure.call(p2));
			}
		};
	}

	@Override
	public int getCountNodesSharingContent() {
	    reportCloneAccess();
		return getDelegate().allClones().size() - 1;
	}

	@Override
	public int getCountNodesSharingContentAndSubtree() {
	    reportCloneAccess();
		return getDelegate().subtreeClones().size() - 1;
	}

	@Override
	public List<? extends Node> getNodesSharingContent() {
	    reportCloneAccess();
		final ArrayList<NodeModel> nodeModels = new ArrayList<NodeModel>(getDelegate().allClones().toCollection());
		nodeModels.remove(getDelegate());
		return ProxyUtils.createNodeList(nodeModels, getScriptContext());
	}

	@Override
	public List<? extends Node> getNodesSharingContentAndSubtree() {
	    reportCloneAccess();
		final ArrayList<NodeModel> nodeModels = new ArrayList<NodeModel>(getDelegate().subtreeClones().toCollection());
		nodeModels.remove(getDelegate());
		return ProxyUtils.createNodeList(nodeModels, getScriptContext());
	}

	@Override
	public Node appendAsCloneWithSubtree(final NodeRO toBeCloned) {
		return appendAsCloneImpl(((NodeProxy) toBeCloned).getDelegate(), true);
	}

	@Override
	public Node appendAsCloneWithoutSubtree(final NodeRO toBeCloned) {
		return appendAsCloneImpl(((NodeProxy) toBeCloned).getDelegate(), false);
	}

	private Node appendAsCloneImpl(final NodeModel toBeCloned, final boolean withSubtree) {
		final NodeModel target = getDelegate();
		final MMapController mapController = (MMapController) getModeController().getMapController();
		if (toBeCloned.getParentNode() == null || toBeCloned.isRoot())
			throw new IllegalArgumentException("can't clone root node or node without parent");
		if (!toBeCloned.getMap().equals(getDelegate().getMap()))
			throw new IllegalArgumentException("can't clone a node from another map");
		if (toBeCloned.subtreeContainsCloneOf(target))
			throw new IllegalArgumentException("can't clone a node which has this node as child");
		final NodeModel clone = withSubtree ? toBeCloned.cloneTree() : toBeCloned.cloneContent();
		mapController.addNewNode(clone, target, target.getChildCount());
		return new NodeProxy(clone, getScriptContext());
	}

	@Override
	public void pasteAsClone() {
		final MMapClipboardController clipboardController = (MMapClipboardController) getClipboardController();
		clipboardController.addClone(clipboardController.getClipboardContents(), getDelegate());
	}

	@Override
	public Node at(final String path) {
		final MMapExplorerController explorer = getExplorer();
		final MapExplorer mapExplorer = explorer.getMapExplorer(getDelegate(), path, accessedNodes());
		try {
			final NodeModel node = mapExplorer.getNode();
			final ScriptContext scriptContext = getScriptContext();
			return new NodeProxy(node, scriptContext);
		}
		catch (NodeNotFoundException e) {
			throw new org.freeplane.api.NodeNotFoundException(e);
		}
	}

	private AccessedNodes accessedNodes() {
		return getScriptContext() != null ? getScriptContext() : AccessedNodes.IGNORE;
	}

	@Override
	public List<? extends Node> allAt(final String path) {
		final MMapExplorerController explorer = getExplorer();
		final MapExplorer mapExplorer = explorer.getMapExplorer(getDelegate(), path, accessedNodes());
		final ArrayList<NodeModel> nodeModels = new ArrayList<NodeModel>(mapExplorer.getNodes());
		final ScriptContext scriptContext = getScriptContext();
		return ProxyUtils.createNodeList(nodeModels, scriptContext);
	}

	@Override
	public String getAlias() {
		final MMapExplorerController explorer = getExplorer();
		return explorer.getAlias(getDelegate());
	}

	@Override
	public boolean getIsGlobal() {
		final MMapExplorerController explorer = getExplorer();
		return explorer.isGlobal(getDelegate());
	}

	@Override
	public void setAlias(final String alias) {
		final MMapExplorerController explorer = getExplorer();
		explorer.setAlias(getDelegate(), alias);
	}

	@Override
	public void setIsGlobal(final boolean value) {
		final MMapExplorerController explorer = getExplorer();
		explorer.makeGlobal(getDelegate(), value);
	}

	@Override
	public DependencyLookup getPrecedents() {
		return new DependencyLookupProxy(getDelegate(), getScriptContext(), DependencySearchStrategy.PRECEDENTS);
	}

	@Override
	public DependencyLookup getDependents() {
		return new DependencyLookupProxy(getDelegate(), getScriptContext(), DependencySearchStrategy.DEPENDENTS);
	}

    @Override
    public NodeGeometry getGeometry() {
        return new NodeGeometryProxy(getDelegate(), getScriptContext());
    }

    @Override
    public String getBranchAsTextOutline() {
        return MindMapPlainTextWriter.INSTANCE.getAsPlainText(Collections.singletonList(getDelegate()), CopiedNodeSet.ALL_NODES);
    }

    @Override
    public void appendTextOutlineAsBranch(String outline) {
        ((MMapClipboardController) getClipboardController()).paste(new StringSelection(outline), getDelegate());
    }

    @Override
    public Tags getTags() {
        return new TagsProxy(getDelegate(), getScriptContext());
    }

	// Node: R/W
	@Override
	public void setBookmark(String name, BookmarkType bookmarkType) {
		final BookmarksController bookmarksController = getBookmarksController();
		final NodeModel node = getDelegate();
		boolean opensAsRoot = (bookmarkType == BookmarkType.ROOT);
		final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(name, opensAsRoot);
		bookmarksController.addBookmark(node, descriptor);
	}

	// Node: R/W
	@Override
	public void removeBookmark() {
		final BookmarksController bookmarksController = getBookmarksController();
		final NodeModel node = getDelegate();
		bookmarksController.removeBookmark(node);
	}

	private BookmarksController getBookmarksController() {
		return getModeController().getExtension(BookmarksController.class);
	}

	// NodeRO: R
	@Override
	public NodeBookmark getBookmark() {
		final BookmarksController bookmarksController = getBookmarksController();
		final MapModel map = getDelegate().getMap();
		final MapBookmarks mapBookmarks = bookmarksController.getBookmarks(map);
		final String nodeId = getDelegate().getID();
		final org.freeplane.features.bookmarks.mindmapmode.NodeBookmark coreBookmark = mapBookmarks.getBookmark(nodeId);
		return coreBookmark != null ? new NodeBookmarkProxy(coreBookmark, getScriptContext()) : null;
	}
}

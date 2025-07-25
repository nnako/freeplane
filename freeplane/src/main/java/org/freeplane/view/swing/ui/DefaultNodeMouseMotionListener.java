package org.freeplane.view.swing.ui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.DoubleClickTimer;
import org.freeplane.core.ui.IMouseListener;
import org.freeplane.core.ui.components.AutoHide;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.FoldingController;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.MouseArea;
import org.freeplane.view.swing.map.NodeView;

/**
 * The MouseMotionListener which belongs to every NodeView
 */
public class DefaultNodeMouseMotionListener implements IMouseListener {
	protected final NodeSelector nodeSelector;
	private static final String FOLD_ON_CLICK_INSIDE = "fold_on_click_inside";
	static final String OPEN_LINKS_ON_PLAIN_CLICKS = "openLinksOnPlainClicks";
	/**
	 * The mouse has to stay in this region to enable the selection after a
	 * given time.
	 */
	protected final DoubleClickTimer doubleClickTimer;
    private boolean popupMenuIsShown;

	public DefaultNodeMouseMotionListener() {
//		mc = modeController;
		doubleClickTimer = new DoubleClickTimer();
		nodeSelector = new NodeSelector();
	}


	protected boolean isInFoldingRegion(MouseEvent e) {
		return ((MainView)e.getComponent()).isInFoldingRegion(e.getPoint());
	}

	protected boolean isInDragRegion(MouseEvent e) {
		return ((MainView)e.getComponent()).isInDragRegion(e.getPoint());
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		if(popupMenuIsShown){
		    return;
		}
		final MainView component = (MainView) e.getComponent();
		NodeView nodeView = component.getNodeView();
		if (nodeView == null)
		    return;

		final NodeModel node = nodeView.getNode();
        final ModeController mc = nodeView.getMap().getModeController();
		final MapController mapController = mc.getMapController();
		if(e.getButton() == MouseEvent.BUTTON1
		        && Compat.isPlainEvent(e)
		        && isInFoldingRegion(e)) {
		    return;
		}

		boolean isDelayedFoldingActive = false;
		final boolean inside = nodeSelector.isInside(e);
		Point point = e.getPoint();
		if(e.getButton() == 1){
            if(Compat.isCtrlEvent(e) || Compat.isPlainEvent(e) && ResourceController.getResourceController().getBooleanProperty(OPEN_LINKS_ON_PLAIN_CLICKS)){
				NamedIcon uiIcon = component.getUIIconAt(point);
				if(uiIcon != null){
					final IconController iconController = mc.getExtension(IconController.class);
					if(iconController.onIconClicked(node, uiIcon))
						return;
				}
				if (component.isClickableLink(point)) {
					LinkController.getController(mc).loadURL(node, e);
					e.consume();
					return;
				}


				final String link = component.getLink(point);
				if (link != null) {
					doubleClickTimer.start(new Runnable() {
						@Override
						public void run() {
							loadLink(node, link);
						}
					});
					e.consume();
					return;
				}
			}
			else if(Compat.isShiftEvent(e)){
		                if (component.isClickableLink(point)) {
		                    mapController.forceViewChange(() -> LinkController.getController(mc).loadURL(node, e));
		                    e.consume();
		                    return;
		                }

		                final String link = component.getLink(point);
		                if (link != null) {
		                    doubleClickTimer.start(new Runnable() {
		                        @Override
		                        public void run() {
		                            mapController.forceViewChange(() -> loadLink(node, link));
		                        }
		                    });
		                    e.consume();
		                    return;
		                }
            }

			if(Compat.isPlainEvent(e)){
				if(inside && (e.getClickCount() == 1 && foldsOnClickInside()
				        || ! (mc.canEdit(node.getMap()) && editsOnDoubleClick()))){
					if (!nodeSelector.shouldSelectOnClick(e)) {
					    isDelayedFoldingActive = true;
						doubleClickTimer.start(new Runnable() {
							@Override
							public void run() {
								MouseEventActor.INSTANCE.withMouseEvent( () ->
									mapController.toggleFoldedAndScroll(node));
							}
						});
					}
				}
			}
			else if(Compat.isShiftEvent(e)){
				if (isInFoldingRegion(e)) {
					if (! mapController.showNextChild(node))
						mapController.fold(node);
					e.consume();
				}
			}
		}

		if (inside && Compat.isCtrlShiftEvent(e) && !nodeSelector.shouldSelectOnClick(e)) {
			doubleClickTimer.cancel();
			MouseEventActor.INSTANCE.withMouseEvent( () ->
				mapController.toggleFoldedAndScroll(node));
			e.consume();
			return;
		}

		if(inside && e.getButton() == 1 &&  ! e.isAltDown()) {
            nodeSelector.extendSelection(e, ! isDelayedFoldingActive);
        }
	}


    private boolean foldsOnClickInside() {
        return ResourceController.getResourceController().getBooleanProperty(FOLD_ON_CLICK_INSIDE);
    }

    protected boolean editsOnDoubleClick() {
        return false;
    }


	private void loadLink(NodeModel node, final String link) {
		try {
			LinkController.getController().loadURI(node, LinkController.createHyperlink(link));
		} catch (Exception ex) {
			LogUtils.warn(ex);
		}
	}

	/**
	 * Invoked when a mouse button is pressed on a component and then
	 * dragged.
	 */
	@Override
	public void mouseDragged(final MouseEvent e) {
		nodeSelector.stopTimerForDelayedSelection();
		if (nodeSelector.isInside(e))
			nodeSelector.extendSelection(e, false);
	}


	private boolean isInFoldingControl(final MouseEvent e) {
		return isInFoldingRegion(e) && ((MainView)e.getComponent()).getFoldingControlBounds().contains(e.getPoint());
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
		if (nodeSelector.isRelevant(e)) {
			nodeSelector.createTimer(e, isInFoldingControl(e));
			mouseMoved(e);
		}
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		nodeSelector.stopTimerForDelayedSelection();
		final MainView v = (MainView) e.getSource();
		v.setMouseArea(MouseArea.OUT);
		nodeSelector.trackWindowForComponent(v);
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		if (!nodeSelector.isRelevant(e))
			return;
		final MainView node = ((MainView) e.getComponent());
		Point point = e.getPoint();
        String link = node.getLink(point);
		boolean followLink = link != null;
		final ModeController modeController = node.getNodeView().getMap().getModeController();
        if(! followLink){
        	followLink = node.isClickableLink(point);
        	if(followLink){
				link = LinkController.getController(modeController).getLinkShortText(node.getNodeView().getNode());
        	}
        }
        final Cursor requiredCursor;
        if(followLink){
        	modeController.getController().getViewController().out(link);
			requiredCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
			node.setMouseArea(MouseArea.LINK);
        }
        else if (isInFoldingControl(e)){
        	requiredCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        	node.setMouseArea(MouseArea.FOLDING);
        }
        else{
        	requiredCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        	node.setMouseArea(MouseArea.DEFAULT);
        }
        if (node.getCursor().getType() != requiredCursor.getType() || requiredCursor.getType() == Cursor.CUSTOM_CURSOR && node.getCursor() != requiredCursor) {
        	node.setCursor(requiredCursor);
        }
		nodeSelector.createTimer(e, isInFoldingControl(e));
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		final MapView mapView = MapView.getMapView(e.getComponent());
		mapView.select();
		doubleClickTimer.cancel();
		popupMenuIsShown = false;
		if (Compat.isPopupTrigger(e)) {
			showPopupMenu(e);
		} else
			if(e.getButton() == MouseEvent.BUTTON1
			&& Compat.isPlainEvent(e)
			&& isInFoldingRegion(e)) {
				final MainView component = (MainView) e.getComponent();
				NodeView nodeView = component.getNodeView();
				if (nodeView == null)
					return;

				final NodeModel node = nodeView.getNode();
				final ModeController mc = nodeView.getMap().getModeController();
				final MapController mapController = mc.getMapController();
				doubleClickTimer.cancel();
				MouseEventActor.INSTANCE.withMouseEvent( () ->
				mapController.toggleFoldedAndScroll(node));
				return;
			}

	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		nodeSelector.stopTimerForDelayedSelection();
        if (Compat.isPopupTrigger(e))
            showPopupMenu(e);
	}

	private void showPopupMenu(final MouseEvent e) {
	    popupMenuIsShown = true;
		final boolean inside = nodeSelector.isInside(e);
		final boolean inFoldingRegion = ! inside && isInFoldingRegion(e);
		if (inside || inFoldingRegion) {
			if(inside){
				nodeSelector.stopTimerForDelayedSelection();
				new NodePopupMenuDisplayer().showNodePopupMenu(e);
			}
			else if(inFoldingRegion){
				showFoldingPopup(e);
			}
		}
	}

	private void showFoldingPopup(MouseEvent e) {
		final NodeView nodeView = nodeSelector.getRelatedNodeView(e);
		ModeController mc = nodeView.getMap().getModeController();
		final FoldingController foldingController = mc.getExtension(FoldingController.class);
		if(foldingController == null)
			return;
		final JPopupMenu popupmenu = foldingController.createFoldingPopupMenu(nodeView.getNode());
		AutoHide.start(popupmenu);
		new NodePopupMenuDisplayer().showMenuAndConsumeEvent(popupmenu, e);
    }

}

package org.freeplane.view.swing.features.filepreview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import org.freeplane.api.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.undo.IActor;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.map.mindmapmode.InsertionRelation;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.NodeHookDescriptor;
import org.freeplane.features.mode.PersistentNodeHook;
import org.freeplane.features.ui.INodeViewLifeCycleListener;
import org.freeplane.features.ui.ViewController;
import org.freeplane.core.util.URIUtils;
import org.freeplane.features.url.UrlManager;
import org.freeplane.n3.nanoxml.XMLElement;
import org.freeplane.view.swing.features.progress.mindmapmode.ProgressIcons;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

@NodeHookDescriptor(hookName = "ExternalObject", //
onceForMap = false)
public class ViewerController extends PersistentNodeHook implements INodeViewLifeCycleListener, IExtension {
    private static final MExternalImageDropListener DTL = new MExternalImageDropListener();
	private static final int BORDER_SIZE = 1;
	public static final Border VIEWER_BORDER_INSTANCE = new ViewerBorder(BORDER_SIZE, Color.BLACK);


	private final class CombiFactory implements IViewerFactory {
		private IViewerFactory factory;

        @Override
        public ScalableComponent createViewer(final URI uri,
                final Dimension preferredSize) throws MalformedURLException,
                IOException {
            factory = getViewerFactory(uri);
            ScalableComponent component = (factory == null ? null : factory.createViewer(uri,
                    preferredSize));
            return component;
        }

        @Override
        public ScalableComponent createViewer(final URI uri,
                final Dimension preferredSize, Runnable callback) throws MalformedURLException,
                IOException {
            factory = getViewerFactory(uri);
            ScalableComponent component = (factory == null ? null : factory.createViewer(uri,
                    preferredSize, callback));
            return component;
        }

		@Override
		public ScalableComponent createViewer(final ExternalResource resource,
				final URI absoluteUri, final int maximumWidth, float zoom)
		        throws MalformedURLException, IOException {
			factory = getViewerFactory(absoluteUri);
			ScalableComponent component = factory.createViewer(resource, absoluteUri,
					maximumWidth, zoom);
			return component;
		}

		@Override
		public String getDescription() {
			final StringBuilder sb = new StringBuilder();
			for (final IViewerFactory factory : factories) {
				if (sb.length() != 0) {
					sb.append(", ");
				}
				sb.append(factory.getDescription());
			}
			return sb.toString();
		}

		@Override
		public boolean accept(final URI uri) {
			return getViewerFactory(uri) != null;
		}

        @Override
        public ScalableComponent createViewer(URI uri, float zoom)
                throws MalformedURLException, IOException {
            factory = getViewerFactory(uri);
            ScalableComponent component = (factory == null ? null : factory.createViewer(uri,
                    zoom));
            return component;
        }

        @Override
        public ScalableComponent createViewer(URI uri, float zoom,
                Runnable callback) throws MalformedURLException, IOException {
            factory = getViewerFactory(uri);
            ScalableComponent component = (factory == null ? null : factory.createViewer(uri,
                    zoom, callback));
            return component;
        }

	}

	static final class FactoryFileFilter extends FileFilter {
		private final IViewerFactory factory;

		protected IViewerFactory getFactory() {
			return factory;
		}

		private FactoryFileFilter(final IViewerFactory factory) {
			this.factory = factory;
		}

		@Override
		public boolean accept(final File f) {
			return f.isDirectory() || factory.accept(f.toURI());
		}

		@Override
		public String getDescription() {
			return factory.getDescription();
		}
	}

	private static final int SENSITIVE_AREA_SIZE = (int) Math.max(10, UITools.FONT_SCALE_FACTOR * 10);
	private class MyMouseListener implements MouseListener, MouseMotionListener {
		private boolean sizeChanged = false;
		private Point basePoint = null;
		private Dimension baseSize = null;
		private boolean isActive() {
			return basePoint != null;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			if (resetSize(e)) {
				return;
			}
			if (showNext(e)) {
				return;
			}
		}

		private boolean resetSize(final MouseEvent e) {
			if (e.getClickCount() != 2) {
				return false;
			}
			final JComponent viewer = (JComponent) e.getComponent();
			final int x = e.getX();
			final int width = viewer.getWidth();
			final int y = e.getY();
			final int height = viewer.getHeight();
			if (x < width - SENSITIVE_AREA_SIZE || y < height - SENSITIVE_AREA_SIZE ) {
				return false;
			}
			final IViewerFactory factory = (IViewerFactory) viewer.getClientProperty(IViewerFactory.class);
			if (factory == null) {
				return true;
			}
			final MapView mapView = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, viewer);
			setZoom(mapView.getModeController(), mapView.getMap(), (ExternalResource) viewer
			    .getClientProperty(ExternalResource.class), 1f);
			sizeChanged = false;
			return true;
		}

		private boolean showNext(final MouseEvent e) {
			//double left click
			final JComponent component = (JComponent) e.getComponent();
			final int cursorType = component.getCursor().getType();
			if ((e.getClickCount() != 2) || (e.getButton() != MouseEvent.BUTTON1)
			        || (cursorType == Cursor.SE_RESIZE_CURSOR)) {
				return false;
			}
			final ExternalResource activeView = getModel(e);
			if(activeView ==  null)
				return false;
			NodeModel node = getNode(e);
			final MapModel map = node.getMap();
			URI absoluteUri = activeView.getAbsoluteUri(map);
			if(absoluteUri == null)
				return false;
			final String sActUri = absoluteUri.toString();
			if (!sActUri.matches(".*_[0-9]{2}\\.[a-zA-Z0-9]*")) {
				return false;
			}
			int i = Integer.parseInt(sActUri.substring(sActUri.lastIndexOf("_") + 1, sActUri.lastIndexOf("_") + 3));
			//show previous with ctrl + double click
			if (e.isControlDown()) {
				if (i > 0) {
					i--;
				}
				else {
					//remove view if 0 and down
					if (activeView.getUri().toString().matches(ProgressIcons.EXTENDED_PROGRESS_ICON_IDENTIFIER)) {
						ProgressIcons.removeProgressIcons(node);
					}
					remove(node, activeView);
					Controller.getCurrentModeController().getMapController().nodeChanged(node,
					    NodeModel.UNKNOWN_PROPERTY, null, null);
					return true;
				}
			}
			else {
				i++;
			}
			final String sNextNum;
			if (i < 10) {
				sNextNum = "0" + Integer.toString(i);
			}
			else {
				sNextNum = Integer.toString(i);
			}
			URI nextUri = null;
			try {
				nextUri = new URI(sActUri.replaceFirst("_[0-9]{2}\\.", "_" + sNextNum + "."));
			}
			catch (final URISyntaxException e1) {
				e1.printStackTrace();
			}
			final String sNextURI = nextUri.getPath();
			if ((sNextURI.contains("_tenth_")&& (i > 10))|| ((sNextURI.contains("_quarter_"))&& (i > 4))) {
				return false;
			}
			final ExternalResource nextView = new ExternalResource(nextUri);
			nextView.setZoom(activeView.getZoom());
			IActor actor = new IActor() {

                @Override
                public void undo() {
                    remove(node, nextView);
                    add(node, activeView);
                }

                @Override
                public String getDescription() {
                    return "updateExtendedProgressIcons";
                }

                @Override
                public void act() {
                    remove(node, activeView);
                    add(node, nextView);
                }
            };
            Controller.getCurrentModeController().execute(actor, map);
			ProgressIcons.updateExtendedProgressIcons(node, sNextURI);
			return true;
		}

        private NodeModel getNode(final MouseEvent e) {
            NodeModel node = null;
			//get node from mouse click
			for (int i = 0; i < e.getComponent().getParent().getComponentCount(); i++) {
				if (e.getComponent().getParent().getComponent(i) instanceof MainView) {
					final MainView mv = (MainView) e.getComponent().getParent().getComponent(i);
					node = mv.getNodeView().getNode();
					break;
				}
			}
			if (node == null) {
				node = Controller.getCurrentModeController().getMapController().getSelectedNode();
			}
            return node;
        }

		@Override
		public void mouseEntered(final MouseEvent e) {
			if (isActive()) {
				return;
			}
			final ExternalResource model = getModel(e);
			if (model == null) {
				return;
			}
			Controller.getCurrentController().getViewController().out(model.getUri().toString());
			setCursor(e);
		}

		private ExternalResource getModel(final MouseEvent e) {
			final JComponent component = (JComponent) e.getComponent();
			final ExternalResource model = (ExternalResource) component.getClientProperty(ExternalResource.class);
			return model;
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			if (isActive()) {
				return;
			}
			setCursor(e.getComponent(), Cursor.DEFAULT_CURSOR);
		}

		private void setCursor(final MouseEvent e) {
			final Component component = e.getComponent();
			final int cursorType;
			final int x = e.getX();
			final int width = component.getWidth();
			final int y = e.getY();
			final int height = component.getHeight();
			if (x >= 0 && x < width  && y >= 0 && y < height
					&& (width - SENSITIVE_AREA_SIZE <= x || height - SENSITIVE_AREA_SIZE <= y )) {
				cursorType = Cursor.SE_RESIZE_CURSOR;
			}
			else {
				cursorType = Cursor.DEFAULT_CURSOR;
			}
			setCursor(component, cursorType);
		}

		private void setCursor(final Component component, final int cursorType) {
			final Cursor cursor = component.getCursor();
			if (cursor.getType() != cursorType) {
				final Cursor predefinedCursor = cursorType == Cursor.DEFAULT_CURSOR ? null : Cursor
				    .getPredefinedCursor(cursorType);
				component.setCursor(predefinedCursor);
			}
			ViewerBorder.repaintBorder((JComponent) component);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			final JComponent component = (JComponent) e.getComponent();
			final int cursorType = component.getCursor().getType();
			if (cursorType == Cursor.SE_RESIZE_CURSOR) {
				final IViewerFactory factory = (IViewerFactory) component.getClientProperty(IViewerFactory.class);
				if (factory == null) {
					return;
				}
				final Point point = e.getPoint();
				this.basePoint = new Point (component.getWidth() - point.x, component.getHeight() - point.y);
				this.baseSize = component.getSize();
				return;
			}
			else {
				imagePopupMenu.maybeShowPopup(e);
				return;
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if (sizeChanged) {
				final JComponent component = (JComponent) e.getComponent();
				final int newWidth = component.getWidth();
				final int newHeight = component.getHeight();
				final float w = baseSize.width;
				final float h = baseSize.height;
				float scalingFactor = Math.max(newWidth / w, newHeight / h);

				if(newWidth != baseSize.width || newHeight != baseSize.height) {
					if (scalingFactor > 0 && Math.abs(scalingFactor - 1) > 0.01) {
						final MapView mapView = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, component);
						final ExternalResource zoomedResource = (ExternalResource) component
								.getClientProperty(ExternalResource.class);
						float newZoom = zoomedResource.getZoom() * scalingFactor;
						setZoom(mapView.getModeController(), mapView.getMap(), zoomedResource, newZoom);
					}
					else {
						component.revalidate();
						component.repaint();
					}
				}
				sizeChanged = false;
			}
			else {
				imagePopupMenu.maybeShowPopup(e);
			}
			this.basePoint = null;
			this.baseSize = null;
			setCursor(e);
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (!isActive()) {
				return;
			}
			final JComponent component = (JComponent) e.getComponent();
			setSize(component, basePoint.x + e.getX(), basePoint.y + e.getY());
		}

		private void setSize(final JComponent component, int newWidth, int newHeight) {
		    setSize(component, newWidth, newHeight, false);
		}

		private void setSize(final JComponent component, int newWidth, int newHeight, boolean isCallRecursive) {
			final int cursorType = component.getCursor().getType();
			sizeChanged = true;
			final Dimension size;
			switch (cursorType) {
				case Cursor.SE_RESIZE_CURSOR:
					newWidth = Math.max(newWidth, 2*BORDER_SIZE);
					newHeight = Math.max(newHeight, 2*BORDER_SIZE);
					final Dimension minimumSize = new Dimension(10, 10);
					final float baseWidth = baseSize.width;
					final float baseHeight = baseSize.height;
					float scalingFactor = Math.max(newWidth / baseWidth, newHeight / baseHeight);
					newWidth = (int) (baseWidth * scalingFactor);
					newHeight = (int) (baseHeight * scalingFactor);
					final MapView mapView = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, component);
					if (! isCallRecursive && scalingFactor < 1) {
						final int minimumWidth = mapView.getZoomed(minimumSize.width);
						final int minimumHeight = mapView.getZoomed(minimumSize.height);
						if (newWidth < minimumWidth || newHeight < minimumHeight) {
							if(baseWidth <= minimumWidth || baseHeight <= minimumHeight)
								size = baseSize;
							else {
								setSize(component, minimumWidth, minimumHeight, true);
								return;
							}
						} else {
							size = new Dimension(newWidth, newHeight);
						}
					} else {
						size = new Dimension(newWidth, newHeight);
					}
					((ScalableComponent) component).setDraftViewerSize(size);
					component.revalidate();
					break;
				default:
			}
			return;
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			if (isActive()) {
				return;
			}
			setCursor(e);
		}

	}

	static private ExternalImagePopupMenu imagePopupMenu;
	private final MyMouseListener mouseListener = new MyMouseListener();
	final private Set<IViewerFactory> factories;
    private final CombiFactory combiFactory;

	public ViewerController() {
		super();
		factories = new HashSet<IViewerFactory>();
		final ModeController modeController = Controller.getCurrentModeController();
		modeController.addINodeViewLifeCycleListener(this);
		modeController.addExtension(this.getClass(), this);
		factories.add(new BitmapViewerFactory());
		combiFactory = new CombiFactory();
	}

	@Override
	protected HookAction createHookAction() {
		return null;
	}

	public void setZoom(final ModeController modeController, final MapModel map, final ExternalResource model,
	                    final float size) {
		final float oldSize = model.getZoom();
		if (size == oldSize) {
			return;
		}
		final IActor actor = new IActor() {
			@Override
			public void act() {
				model.setZoom(size);
				modeController.getMapController().mapSaved(map, false);
			}

			@Override
			public String getDescription() {
				return "setModelSize";
			}

			@Override
			public void undo() {
				model.setZoom(oldSize);
				modeController.getMapController().mapSaved(map, false);
			}
		};
		modeController.execute(actor, map);
	}

	@Override
	protected void add(final NodeModel node, final IExtension extension) {
		final ExternalResource preview = (ExternalResource) extension;
		for(NodeModel nodeClone : node.allClones()){
			for (final INodeView iNodeView : nodeClone.getViewers()) {
				final NodeView view = (NodeView) iNodeView;
				createViewer(preview, view);
			}
		}
		super.add(node, extension);
	}

	@Override
	protected IExtension createExtension(final NodeModel node) {
		URI uri = createURI(node);
		if(uri == null)
			return null;
		File input = new File(uri.getPath());
		final ExternalResource preview = new ExternalResource(uri);
		ProgressIcons.updateExtendedProgressIcons(node, input.getName());
		return preview;
	}

	protected URI createURI(final NodeModel node) {
		final Controller controller = Controller.getCurrentController();
		final ViewController viewController = controller.getViewController();
		final MapModel map = node.getMap();
		final File file = map.getFile();
		final boolean useRelativeUri = ResourceController.getResourceController().getProperty("links").equals(
		    "relative");
		if (file == null && useRelativeUri) {
			JOptionPane.showMessageDialog(viewController.getCurrentRootComponent(), TextUtils
			    .getText("not_saved_for_image_error"), "Freeplane", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		final UrlManager urlManager = controller.getModeController().getExtension(UrlManager.class);
		final JFileChooser chooser = urlManager.getFileChooser();
		chooser.setAcceptAllFileFilterUsed(false);
		final FileFilter fileFilter;
		if (factories.size() > 1) {
			fileFilter = getCombiFileFilter();
			chooser.addChoosableFileFilter(fileFilter);
			for (final IViewerFactory factory : factories) {
				chooser.addChoosableFileFilter(new FactoryFileFilter(factory));
			}
		}
		else {
			fileFilter = new FactoryFileFilter(factories.iterator().next());
		}
		chooser.setFileFilter(fileFilter);
		chooser.putClientProperty(FactoryFileFilter.class, fileFilter);
		chooser.setAccessory(new ImagePreview(chooser));
		final int returnVal = chooser.showOpenDialog(Controller.getCurrentController().getViewController()
		    .getCurrentRootComponent());
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		final File input = chooser.getSelectedFile();
		if (input == null) {
			return null;
		}
		URI uri = uriOf(input);
		if (uri == null) {
			return null;
		}
		if (useRelativeUri && uri.getScheme().equals("file")) {
			uri = LinkController.toLinkTypeDependantURI(map.getFile(), input);
		}
		return uri;
	}


	private URI uriOf(final File input) {
		String path = input.getPath();
		try {
	        for (String protocol : new String[]{"http:" + File.separatorChar, "https:" + File.separatorChar}){
	        	int uriStart = path.indexOf(protocol);
	        	if(uriStart != -1)
	        		return new URI(protocol.substring(0, protocol.length() - 1) + "//" + path.substring(uriStart + protocol.length()).replace('\\', '/'));
	        }
        }
        catch (URISyntaxException e) {
        	LogUtils.warn(e);
        }
	    return input.toURI();
    }

	private IViewerFactory getViewerFactory(final URI uri) {
		for (final IViewerFactory factory : factories) {
			if (factory.accept(uri)) {
				return factory;
			}
		}
		return null;
	}

	@Override
	protected IExtension createExtension(final NodeModel node, final XMLElement element) {
		try {
			final String attrUri = element.getAttribute("URI", null);
			if (attrUri != null) {
				final URI uri = URIUtils.createURIFromString(attrUri);
				if (uri != null) {
					final ExternalResource previewUrl = new ExternalResource(uri);
					final String attrSize = element.getAttribute("SIZE", null);
					if (attrSize != null) {
						final float size = Float.parseFloat(attrSize);
						previewUrl.setZoom(size);
					}
					Controller.getCurrentModeController().getMapController().nodeChanged(node);
					return previewUrl;
				}
			}
		}
		catch (final URISyntaxException e) {
			LogUtils.warn("Failed to create URI from attribute: " + element.getAttribute("URI", null), e);
		}
		return null;
	}

	void createViewer(final ExternalResource resource, final NodeView view) {
		MapView map = view.getMap();
        final JComponent viewer = createViewer(map.getMap(), resource, map.getZoom());
		if (imagePopupMenu == null) {
			imagePopupMenu = new ExternalImagePopupMenu();
		}
		viewer.setBorder(VIEWER_BORDER_INSTANCE);
		final Set<NodeView> viewers = resource.getViewers();
		viewers.add(view);
		viewer.setBounds(viewer.getX() - 5, viewer.getY() - 5, viewer.getWidth() + 15, viewer.getHeight() + 15);
		view.addContent(viewer, NodeView.IMAGE_VIEWER_POSITION);
		if(map.getModeController().canEdit()){
			final DropTarget dropTarget = new DropTarget(viewer, DTL);
			dropTarget.setActive(true);
		}
		if(view.isShortened())
			viewer.setVisible(false);
		else {
			viewer.revalidate();
			viewer.repaint();
		}
	}

	void deleteViewer(final ExternalResource model, final NodeView nodeView) {
		final Set<NodeView> viewers = model.getViewers();
		if (!viewers.contains(nodeView)) {
			return;
		}
		nodeView.removeContent(NodeView.IMAGE_VIEWER_POSITION);
		viewers.remove(nodeView);
	}

	@Override
	protected Class<ExternalResource> getExtensionClass() {
		return ExternalResource.class;
	}

	@Override
	public void onViewCreated(final Container container) {
		final NodeView nodeView = (NodeView) container;
		final ExternalResource previewUri = nodeView.getNode().getExtension(ExternalResource.class);
		if (previewUri == null) {
			return;
		}
		createViewer(previewUri, nodeView);
	}

	@Override
	public void onViewRemoved(final Container container) {
		final NodeView nodeView = (NodeView) container;
		final ExternalResource previewUri = nodeView.getNode().getExtension(ExternalResource.class);
		if (previewUri == null) {
			return;
		}
		deleteViewer(previewUri, nodeView);
	}

	@Override
	protected void remove(final NodeModel node, final IExtension extension) {
		final ExternalResource resource = (ExternalResource) extension;
		resource.removeViewers();
		super.remove(node, extension);
	}

	@Override
	protected void saveExtension(final IExtension extension, final XMLElement element) {
		final ExternalResource previewUri = (ExternalResource) extension;
		final URI uri = previewUri.getUri();
		if (uri != null) {
			element.setAttribute("URI", uri.toString());
		}
		final float size = previewUri.getZoom();
		if (size != -1) {
			element.setAttribute("SIZE", Float.toString(size));
		}
		super.saveExtension(extension, element);
	}

	private JComponent createViewer(final MapModel map, final ExternalResource model, float zoom) {
		final URI uri = model.getUri();
		if (uri == null) {
			return new JLabel("no file set");
		}
		final URI absoluteUri = model.getAbsoluteUri(map);
		if (absoluteUri == null) {
			return new JLabel(uri.toString());
		}
		final IViewerFactory factory = getViewerFactory(absoluteUri);
		if (factory == null) {
			return new JLabel(uri.toString());
		}
		JComponent viewer = null;
		try {
			final int maxWidth = ResourceController.getResourceController().getIntProperty("max_image_width");
			viewer = (JComponent) factory.createViewer(model, absoluteUri, maxWidth, zoom);
		}
		catch (final Exception e) {
			final String info = HtmlUtils.combineTextWithExceptionInfo(uri.toString(), e);
			final JLabel errorLabel = new JLabel(info);
			errorLabel.addMouseListener(mouseListener);
			return errorLabel;
		}
		if (viewer == null) {
			return new JLabel(uri.toString());
		}
		viewer.putClientProperty(IViewerFactory.class, factory);
		viewer.putClientProperty(ExternalResource.class, model);
		viewer.addMouseListener(mouseListener);
		viewer.addMouseMotionListener(mouseListener);
		return viewer;
	}

	private FileFilter getCombiFileFilter() {
		return new FactoryFileFilter(combiFactory);
	}

	public void addFactory(final IViewerFactory factory) {
		factories.add(factory);
	}

	public void removeFactory(final IViewerFactory factory) {
		factories.remove(factory);
	}

	public boolean pasteImage(URI uri, final NodeModel node) {
		return pasteImage(uri, node, PasteMode.INSIDE);
	}

	/**
	 * This method attaches an image to a node, that is referenced with an uri
	 * @param uri : The image that is to be attached to a node
	 * @param node : The node that is worked upon
	 * @return : true if successful, false otherwise
	 */
	public boolean paste(final URI uri, final NodeModel node) {

		if (uri == null || getViewerFactory(uri) == null) {
			return false;
		}

		final ExternalResource preview = new ExternalResource(uri);
		undoableDeactivateHook(node);
		undoableActivateHook(node, preview);
		ProgressIcons.updateExtendedProgressIcons(node, uri.getPath());
		return true;
	}

	public static enum PasteMode{
		AS_SIBLING_BEFORE(InsertionRelation.AS_SIBLING_BEFORE, Side.AS_SIBLING_BEFORE),
		AS_SIBLING_AFTER(InsertionRelation.AS_SIBLING_AFTER, Side.AS_SIBLING_AFTER),
		AS_CHILD(InsertionRelation.AS_CHILD, Side.DEFAULT),
		INSIDE(null, null);

		static private final EnumMap<Side, PasteMode> bySide = new EnumMap<>(Side.class);
		static {
			bySide.put(Side.AS_SIBLING_BEFORE, AS_SIBLING_BEFORE);
			bySide.put(Side.AS_SIBLING_AFTER, AS_SIBLING_AFTER);
		}
		public static PasteMode bySide(Side side) {
			return bySide.getOrDefault(side, AS_CHILD);
		}

		public final InsertionRelation insertionRelation;
		public final Side side;
		private PasteMode(InsertionRelation insertionRelation, Side side) {
			this.insertionRelation = insertionRelation;
			this.side = side;
		}


	}

	public boolean paste(final File file, final NodeModel targetNode, final PasteMode mode) {
		URI uri = uriOf(file);
		return pasteImage(uri, targetNode, mode);
	}

	public boolean pasteImage(URI uri, final NodeModel targetNode, final PasteMode mode) {
	    if (uri == null || getViewerFactory(uri) == null) {
			return false;
		}
		File file = new File(uri.getPath());
		boolean isFile = uri.getScheme().equals("file");
		if (isFile) {
	        if (!file.exists()) {
	        	return false;
	        }
	        final File mapFile = targetNode.getMap().getFile();
	        if (mapFile == null && LinkController.getLinkType() == LinkController.LINK_RELATIVE_TO_MINDMAP) {
	        	JOptionPane.showMessageDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
	        		TextUtils.getText("not_saved_for_image_error"), "Freeplane", JOptionPane.WARNING_MESSAGE);
	        	return false;
	        }
	        if (LinkController.getLinkType() != LinkController.LINK_ABSOLUTE) {
	        	uri = LinkController.toLinkTypeDependantURI(mapFile, file);
	        }
        }
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		final NodeModel node;
		if (mode.equals(PasteMode.INSIDE)) {
			node = targetNode;
		}
		else {
			node = mapController.newNode(file.getName(), targetNode.getMap());
			InsertionRelation relation =  mode.insertionRelation;
			node.setSide(MapController.suggestNewChildSide(targetNode, mode.side));
			mapController.insertNode(node, targetNode, relation);
		}
		final ExternalResource preview = new ExternalResource(uri);
		undoableDeactivateHook(node);
		undoableActivateHook(node, preview);
		ProgressIcons.updateExtendedProgressIcons(node, file.getName());
		return true;
    }

	public IViewerFactory getViewerFactory() {
	    return combiFactory;
    }
}

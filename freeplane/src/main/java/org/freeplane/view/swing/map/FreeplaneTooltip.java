package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.JToolTip;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.features.filepreview.IViewerFactory;
import org.freeplane.view.swing.features.filepreview.ImageRendererFactory;
import org.freeplane.view.swing.features.filepreview.ViewerController;

@SuppressWarnings("serial")
public class FreeplaneTooltip extends JToolTip {
	public static final String TEXT_HTML = "text/html";

	private final GraphicsConfiguration graphicsConfiguration;
	private final String contentType;
	private URL baseUrl;

    private final boolean honorDisplayProperties;

	public FreeplaneTooltip(GraphicsConfiguration graphicsConfiguration, String contentType, boolean honorDisplayProperties){
		this.graphicsConfiguration = graphicsConfiguration;
		this.contentType = contentType;
        this.honorDisplayProperties = honorDisplayProperties;
	}

	@Override
    public void setTipText(String tipText) {
		final Dimension tooltipSize = tooltipSize(graphicsConfiguration);
		try {
			final URI uri = new URI(tipText);
			final ViewerController viewerController = Controller.getCurrentModeController().getExtension(ViewerController.class);
			if(viewerController != null) {
				final IViewerFactory viewerFactory = viewerController.getViewerFactory();
				if (viewerFactory.accept(uri)) {
					final URI absoluteUri = uri.isAbsolute() ? uri : baseUrl.toURI().resolve(uri);
					if(! absoluteUri.getScheme().equals("file") || new File(absoluteUri).canRead()) {
						final JComponent imageViewer = new ImageRendererFactory().createRenderer(viewerFactory, absoluteUri, tooltipSize);
						add(imageViewer);
						return;
					}
				}
			}
		}
		catch (URISyntaxException e) {
			// fall through
		}
		final TextualTooltipRendererFactory tooltipScrollPaneFactory = new TextualTooltipRendererFactory(contentType, baseUrl, tipText, getComponent(), tooltipSize, honorDisplayProperties);
		add(tooltipScrollPaneFactory.getTooltipRenderer());
	}

	private Dimension tooltipSize(GraphicsConfiguration graphicsConfiguration) {
		final Rectangle screenBounds = graphicsConfiguration.getBounds();
		final int screenHeight = screenBounds.height - 80;
		final int screenWidth = screenBounds.width - 80;
		final int maximumHeight = Math.min(screenHeight, getIntProperty("toolTipManager.max_tooltip_height"));
		int maximumWidth = Math.min(screenWidth, getIntProperty("toolTipManager.max_tooltip_width"));
		final Dimension maximumSize = new Dimension(maximumWidth, maximumHeight);
		return maximumSize;
	}
	private int getIntProperty(String propertyName) {
		return ResourceController.getResourceController().getIntProperty(propertyName, Integer.MAX_VALUE);
	}

	@Override
    public Dimension getPreferredSize() {
	    final Component renderer = getComponent(0);
		return renderer.getPreferredSize();
    }

	@Override
    public void doLayout() {
		final Component renderer = getComponent(0);
		renderer.setSize(getSize());
	    super.doLayout();
    }

	public void setBase(URL url) {
		this.baseUrl = url;
	}

}

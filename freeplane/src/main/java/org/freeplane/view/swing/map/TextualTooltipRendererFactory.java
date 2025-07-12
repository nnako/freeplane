package org.freeplane.view.swing.map;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;
import java.security.AccessControlException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.freeplane.core.ui.components.JRestrictedSizeScrollPane;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.components.html.SynchronousScaledEditorKit;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;

class TextualTooltipRendererFactory {
	final private JEditorPane tip;
	private int maximumWidth;
	private final String contentType;
	private final JRestrictedSizeScrollPane scrollPane;
	private final JComponent component;
	private final URL baseUrl;
	TextualTooltipRendererFactory(
	        String contentType, URL baseUrl, String tipText, JComponent component,
	        ComponentOrientation componentOrientation, Dimension tooltipSize, boolean honorDisplayProperties){
		this.contentType = contentType;
		this.baseUrl = baseUrl;
		this.component = component;
		tip  = new JEditorPane();
		tip.setContentType(contentType);
		tip.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, honorDisplayProperties);
		if(contentType.equals(FreeplaneTooltip.TEXT_HTML)) {
			final HTMLEditorKit kit = SynchronousScaledEditorKit.create();
			tip.setEditorKit(kit);
			final HTMLDocument document = (HTMLDocument) tip.getDocument();
			document.setPreservesUnknownTags(false);
			final StyleSheet styleSheet = document.getStyleSheet();
			styleSheet.addRule("p {margin-top:0;}");
			styleSheet.addRule("table {border: 0; border-spacing: 0;}");
			styleSheet.addRule("th, td {border: 1px solid;}");
		}
		tip.setComponentOrientation(componentOrientation);
		tip.setEditable(false);
		tip.setMargin(new Insets(0, 0, 0, 0));
		final LinkOpener linkOpener = new LinkOpener(this::getNode);
		tip.addMouseListener(linkOpener);
		tip.addMouseMotionListener(linkOpener);

		scrollPane = new JRestrictedSizeScrollPane(tip);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		final int scrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
		tooltipSize.width -= scrollBarWidth;
		scrollPane.setMaximumSize(tooltipSize);
		maximumWidth = tooltipSize.width;
		UITools.setScrollbarIncrement(scrollPane);
		tip.setOpaque(true);
		scrollPane.addComponentListener(new ComponentAdapter() {

			@Override
            public void componentResized(ComponentEvent e) {
				scrollUp();
				scrollPane.removeComponentListener(this);
            }

		});
		setTipText(tipText);

	}

	private NodeModel getNode() {
		NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, component);
		return nodeView != null ? nodeView.getNode() : null;
	}

    private void setTipText(String tipText) {
		try{
        	setTipTextUnsafe(tipText);
        }
        catch (Exception e1) {
        	if(e1 instanceof AccessControlException)
        		LogUtils.warn(e1.getMessage());
        	else
        		LogUtils.severe(e1);
            final String localizedMessage = e1.getLocalizedMessage();
        	final String htmlEscapedText = HtmlUtils.plainToHTML(localizedMessage + '\n' + tipText);
        	try{
        		setTipTextUnsafe(htmlEscapedText);
        	}
        	catch (Exception e2){
        	}
        }
    }

	private void setTipTextUnsafe(String tipText) throws Exception{
		tip.setSize(0, 0);
		tip.setPreferredSize(null);
		tip.setText(tipText);
		((HTMLDocument)tip.getDocument()).setBase(baseUrl);
		Dimension preferredSize = tip.getPreferredSize();
		if (preferredSize.width > maximumWidth && contentType.equals(FreeplaneTooltip.TEXT_HTML)) {
			final HTMLDocument document = (HTMLDocument) tip.getDocument();
			document.getStyleSheet().addRule("body { width: " + maximumWidth  + "}");
			// bad hack: call "setEditable" only to update view
			tip.setEditable(true);
			tip.setEditable(false);
			preferredSize = tip.getPreferredSize();
			if (preferredSize.width > maximumWidth) {

			}
		}
		tip.setSize(preferredSize);
		preferredSize = tip.getPreferredSize();
		tip.setPreferredSize(preferredSize);

	}

	JComponent getTooltipRenderer() {
		return scrollPane;
	}
	private void scrollUp() {
		tip.scrollRectToVisible(new Rectangle(1, 1));
    }

}

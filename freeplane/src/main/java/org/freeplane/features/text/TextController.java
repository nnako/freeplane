/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
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
package org.freeplane.features.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.Icon;

import org.freeplane.api.TextWritingDirection;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.html.CssRuleBuilder;
import org.freeplane.core.util.HtmlProcessor;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.format.IFormattedObject;
import org.freeplane.features.format.PatternFormat;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.ITooltipProvider;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeSizeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.styles.IStyle;
import org.freeplane.features.styles.LogicalStyleController;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.features.text.IContentTransformer.Mode;
import org.freeplane.view.swing.map.MainView;

/**
 * @author Dimitry Polivaev
 */
public class TextController implements IExtension {
    public static final String CONTENT_TYPE_HTML = "html";
    public static final String CONTENT_TYPE_AUTO = "auto";

    public static final String DETAILS_HIDDEN = "DETAILS_HIDDEN";
	public static final String FILTER_NODE = "filter_node";
	public static final String FILTER_ANYTEXT = "filter_any_text";
	public static final String FILTER_NOTE = "filter_note";
	public static final String FILTER_PARENT_TEXT = "filter_parent_text";
	public static final String FILTER_DETAILS = "filter_details";
	private static final Integer NODE_TOOLTIP = 1;
	private static final Integer DETAILS_TOOLTIP = 2;
	public static final String MARK_TRANSFORMED_TEXT = "highlight_formulas";
	private final List<IContentTransformer> textTransformers;
	protected final ModeController modeController;
	private boolean nodeNumberingEnabled = true;

	public static boolean isMarkTransformedTextSet() {
		return Controller.getCurrentController().getResourceController().getBooleanProperty(MARK_TRANSFORMED_TEXT);
	}

    public static boolean isHtmlContentType(String noteContentType) {
        return TextController.CONTENT_TYPE_AUTO.equals(noteContentType)
                || TextController.CONTENT_TYPE_HTML.equals(noteContentType);
    }

	public static TextController getController() {
		final ModeController modeController = Controller.getCurrentModeController();
		return getController(modeController);
	}

	public static TextController getController(ModeController modeController) {
		return modeController.getExtension(TextController.class);
	}

	public static void install() {
		FilterController.getCurrentFilterController().getConditionFactory().addConditionController(5,
		    new NodeTextConditionController());
	}

	public void install(final ModeController modeController) {
		modeController.addExtension(TextController.class, this);
	}

	public TextController(final ModeController modeController) {
		super();
		textTransformers = new LinkedList<IContentTransformer>();
		this.modeController = modeController;
		final MapController mapController = modeController.getMapController();
		final ReadManager readManager = mapController.getReadManager();
		final WriteManager writeManager = mapController.getWriteManager();
		final NodeTextBuilder textBuilder = new NodeTextBuilder();
		textBuilder.registerBy(readManager, writeManager);
		writeManager.addExtensionElementWriter(DetailModel.class, textBuilder);
		writeManager.addExtensionAttributeWriter(ShortenedTextModel.class, textBuilder);
		modeController.addAction(new ToggleDetailsAction());
		modeController.addAction(new SetShortenerStateAction());
		//		modeController.addAction(new ToggleNodeNumberingAction());
		// this IContentTransformer is unconditional because its outcome
		// is explicitly defined by the user (assigning a format)!
		addTextTransformer(new FormatContentTransformer(this, 50));
		registerDetailsTooltip();
		registerNodeTextTooltip();
	}

    public void addTextTransformer(IContentTransformer textTransformer) {
		textTransformers.add(textTransformer);
		Collections.sort(textTransformers);
	}

	public List<IContentTransformer> getTextTransformers() {
		return textTransformers;
	}

	public void removeTextTransformer(IContentTransformer textTransformer) {
		textTransformers.remove(textTransformer);
	}

	public String getText(NodeModel nodeModel) {
		return nodeModel.getText();
	}

    public Object getTransformedObject(final NodeModel node, Object nodeProperty, Object content, Component component)
            throws TransformationException{
        return getTransformedObject(node, nodeProperty, content, Mode.VIEW, component);
    }

	private Object getTransformedObject(final NodeModel node, Object nodeProperty, Object content, Mode mode, Component component)
	        throws TransformationException {
		if (content instanceof String) {
			String string = (String) content;
			if (string.length() > 0 && string.charAt(0) == '\'') {
				if (node != null && nodeProperty == node && isTextFormattingDisabled(node))
					return string;
				else
					return string.substring(1);
			}
		}
		boolean markTransformation = false;
		for (IContentTransformer textTransformer : getTextTransformers()) {
			try {
				Object in = content;
				content = textTransformer.transformContent(node, nodeProperty, in, this, mode, component);
				markTransformation = markTransformation || textTransformer.markTransformation() && !in.equals(content);
			}
			catch (RuntimeException e) {
				throw new TransformationException(e);
			}
		}
		if (markTransformation)
			return new HighlightedTransformedObject(content);
		else
			return content;
	}

	public boolean isFormula(Object content) {
		if (content instanceof String) {
			String string = (String) content;
			if (string.length() > 0 && string.charAt(0) == '\'') {
				return false;
			}
		}
		for (IContentTransformer textTransformer : getTextTransformers()) {
			if (textTransformer.isFormula(content))
				return true;
		}
		return false;
	}

	public Icon getIcon(Object object) {
		if (object instanceof HighlightedTransformedObject) {
			return getIcon(((HighlightedTransformedObject) object).getObject());
		}
		return object instanceof Icon ? (Icon) object : null;
	}

	public boolean isTextFormattingDisabled(final NodeModel nodeModel) {
		return PatternFormat.IDENTITY_PATTERN.equals(getNodeFormat(nodeModel));
	}

    public Object getTransformedObjectNoFormattingNoThrow(final NodeModel node, Object nodeProperty, Object data) {
        return getTransformedObjectNoFormattingNoThrow(node, nodeProperty, data, Mode.VIEW);
    }

	private Object getTransformedObjectNoFormattingNoThrow(final NodeModel node, Object nodeProperty, Object data, Mode mode) {
		try {
			Object transformedObject = getTransformedObject(node, nodeProperty, data, mode, null);
			if (transformedObject instanceof HighlightedTransformedObject)
				transformedObject =  ((HighlightedTransformedObject) transformedObject).getObject();
			if (transformedObject instanceof IFormattedObject)
				transformedObject =  ((IFormattedObject) transformedObject).getObject();
			return transformedObject;
		}
		catch (Throwable e) {
			LogUtils.warn(e.getMessage());
			return TextUtils.format("MainView.errorUpdateText", data, e.getLocalizedMessage());
		}
	}

	public Object getTransformedObject(NodeModel node) throws TransformationException {
		return getTransformedObject(node, null);
	}
	public Object getTransformedObject(NodeModel node, Component component) throws TransformationException {
		final Object userObject = node.getUserObject();
		return getTransformedObject(node, node, userObject, component);
	}

	public Object getTransformedObjectNoThrow(NodeModel node) {
		final Object userObject = node.getUserObject();
		return getTransformedObjectNoFormattingNoThrow(node, node, userObject);
	}

	/** convenience method for getTransformedText().toString. */
	public String getTransformedText(final NodeModel node, Object nodeProperty, Object data)
	        throws TransformationException {
		Object transformed = getTransformedObject(node, nodeProperty, data, null);
		if(transformed instanceof Icon)
			return data.toString();
		else
			return transformed.toString();
	}

    public String getTransformedTextNoThrow(final NodeModel node, Object nodeProperty, Object data) {
        Object result = getTransformedObjectNoFormattingNoThrow(node, nodeProperty, data);
        return result.toString();
    }
    public String getTransformedTextForClipboard(final NodeModel node, Object nodeProperty, Object data) {
        return getTransformedObjectNoFormattingNoThrow(node, nodeProperty, data, Mode.TEXT).toString();
    }

	public boolean isMinimized(NodeModel node) {
		final ShortenedTextModel shortened = ShortenedTextModel.getShortenedTextModel(node);
		return shortened != null;
	}

	/** returns transformed text converted to plain text. */
	public String getPlainTransformedText(NodeModel nodeModel) {
		return HtmlUtils.htmlToPlain(getTransformedTextNoThrow(nodeModel));
	}

	public String getPlainTransformedTextWithoutNodeNumber(NodeModel node) {
		return withNodeNumbering( false, () -> getPlainTransformedText(node));
	}

	public <T> T withNodeNumbering(boolean isEnabled, Supplier<T> supplier) {
		final boolean nodeNumberingWasEnabled = nodeNumberingEnabled;
		nodeNumberingEnabled = isEnabled;
		try {
			return supplier.get();
		}
		finally {
			nodeNumberingEnabled = nodeNumberingWasEnabled;
		}

	}

	public String getTransformedTextNoThrow(NodeModel nodeModel) {
		return getTransformedTextNoThrow(nodeModel, nodeModel, nodeModel.getUserObject());
	}

	public String getShortPlainText(NodeModel nodeModel, int maximumCharacters, String continuationMark) {
		String adaptedText = getPlainTransformedTextWithoutNodeNumber(nodeModel);
		String shortText = TextUtils.getShortText(adaptedText, maximumCharacters, continuationMark);
		TextWritingDirection textWritingDirection = modeController.getExtension(NodeStyleController.class).getTextWritingDirection(nodeModel);
		return textWritingDirection == TextWritingDirection.RIGHT_TO_LEFT ? TextWritingDirection.RIGHT_TO_LEFT.embedded(shortText) : shortText;
	}

	public String getShortPlainText(NodeModel nodeModel) {
		return getShortPlainText(nodeModel, 40, " ...");
	}

	public String getShortText(String longText) {
		String text;
		final boolean isHtml = HtmlUtils.isHtml(longText);
		HtmlProcessor htmlProcessor = null;

		if (isHtml) {
		    htmlProcessor = new HtmlProcessor(longText);
			text = htmlProcessor.getText();
		}
		else {
			text = longText;
		}
		int skippedCharacterCount = countInitialSpaces(text);
		int length = text.length() - skippedCharacterCount;
		final int eolPosition;
		if(isHtml) {
            int lineBreakIndex = HtmlUtils.htmlToPlain(longText).indexOf('\n');
            eolPosition = lineBreakIndex >= 0 ? skippedCharacterCount + lineBreakIndex : text.length();
        } else {
            int lineBreakIndex = text.indexOf('\n', skippedCharacterCount);
            eolPosition = lineBreakIndex >= 0 ? lineBreakIndex : text.length();
        }
		final int maxShortenedNodeWidth = ResourceController.getResourceController()
		    .getIntProperty("max_shortened_text_length");
		if (eolPosition >= length || eolPosition >= maxShortenedNodeWidth) {
			if (length <= maxShortenedNodeWidth) {
				return isHtml ? longText : longText.substring(skippedCharacterCount);
			}
			length = maxShortenedNodeWidth;
		}
		else {
			length = eolPosition;
		}
		if (htmlProcessor != null)
			return htmlProcessor.htmlSubstring(0, skippedCharacterCount + length);
		else
		    return text.substring(skippedCharacterCount, skippedCharacterCount + length);
	}

	private int countInitialSpaces(String text) {
	    int count = 0;
	    for(int i = 0; i < text.length(); i++) {
	        if(Character.isWhitespace(text.charAt(i)))
	            count++;
	        else
	            return count;
	    }
	    return count;
	}

    public String getDetailsContentType(NodeModel node) {
	    Collection<IStyle> collection = LogicalStyleController.getController(modeController).getStyles(node, StyleOption.FOR_UNSELECTED_NODE);
	    final MapStyleModel model = MapStyleModel.getExtension(node.getMap());
	    for(IStyle styleKey : collection){
	        final NodeModel styleNode = model.getStyleNode(styleKey);
	        if (styleNode == null) {
	            continue;
	        }
	        final DetailModel details = DetailModel.getDetail(styleNode);
	        if (details != null) {
	            String contentType = details.getContentType();
	            if (contentType != null) {
	                return contentType;
	            }
	        }
	    }
        return TextController.CONTENT_TYPE_HTML;
	}


	public void setDetailsHidden(NodeModel node, boolean isHidden) {
		final DetailModel details = DetailModel.createDetailText(node);
		if (isHidden == details.isHidden()) {
			return;
		}
		details.setHidden(isHidden);
		node.addExtension(details);
		final NodeChangeEvent nodeChangeEvent = new NodeChangeEvent(node, DETAILS_HIDDEN, !isHidden, isHidden, true, false);
		Controller.getCurrentModeController().getMapController().nodeRefresh(nodeChangeEvent);
	}

	private void registerDetailsTooltip() {
		modeController.addToolTipProvider(DETAILS_TOOLTIP, new ITooltipProvider() {
			@Override
			public String getTooltip(final ModeController modeController, NodeModel node, Component view, TooltipTrigger tooltipTrigger) {
				final DetailModel details = DetailModel.getDetail(node);
				if (!providesTooltip(node, details, tooltipTrigger)) {
					return null;
				}
				final String htmlBodyStyle = (view instanceof MainView)
						? getTooltipHtmlStyle(modeController, node, (MainView) view) : "";
				String data = details.getText();
				String text;
				try {
					final Object transformed = TextController.getController().getTransformedObjectNoFormattingNoThrow(node, details, data);
					text = HtmlUtils.objectToHtml(transformed);
				}
				catch (Exception e) {
					text = TextUtils.format("MainView.errorUpdateText", data, e.getLocalizedMessage());
				}
				if (!HtmlUtils.isHtml(text)) {
					text = HtmlUtils.plainToHTML(text);
				}

				final String tooltipText = htmlBodyStyle.isEmpty() ? text : text.replaceFirst("<body>", "<body><div style=\"" + htmlBodyStyle + "\">")
				    .replaceFirst("</body>", "</div></body>");
				return tooltipText;
			}

			private boolean providesTooltip(NodeModel node, final DetailModel details,
			        TooltipTrigger tooltipTrigger) {
				return details != null
						&& ! details.getTextOr("").isEmpty()
						&& (tooltipTrigger == TooltipTrigger.LINK
							|| details.isHidden() ||
							ShortenedTextModel.isShortened(node));
			}

			private String getTooltipHtmlStyle(final ModeController modeController, NodeModel node, MainView view) {
				final NodeStyleController style = modeController.getExtension(NodeStyleController.class);
				final MapStyleModel model = MapStyleModel.getExtension(node.getMap());
				final NodeModel detailStyleNode = model.getStyleNodeSafe(MapStyleModel.DETAILS_STYLE);
				Font detailFont = style.getFont(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE);
				Color detailBackground = style.getBackgroundColor(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE);
				Color detailForeground = style.getColor(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE);
				final int alignment = style.getHorizontalTextAlignment(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE).swingConstant;
				float zoom = view.getNodeView().getMap().getZoom();
				final String htmlBodyStyle = new StringBuilder()
				    .append(new CssRuleBuilder()
				        .withHTMLFont(detailFont)
				        .withColor(detailForeground)
				        .withBackground(detailBackground)
				        .withAlignment(alignment)
				        .withMaxWidthAsPt(zoom, NodeSizeModel.getMaxNodeWidth(detailStyleNode),
				        style.getMaxWidth(node, StyleOption.FOR_UNSELECTED_NODE))).toString();
				return htmlBodyStyle;
			}
		});
	}

	private void registerNodeTextTooltip() {
		modeController.addToolTipProvider(NODE_TOOLTIP, new ITooltipProvider() {
			@Override
			public String getTooltip(final ModeController modeController, NodeModel node, Component view, TooltipTrigger tooltipTrigger) {
				if (tooltipTrigger != TooltipTrigger.LINK && !ShortenedTextModel.isShortened(node)) {
					return null;
				}
				final String htmlBodyStyle = (view instanceof MainView)
						? getTooltipHtmlStyle(modeController, node, (MainView) view) : "";
				final Object data = node.getUserObject();
				String text;
				try {
					final Object transformed = TextController.getController().getTransformedObjectNoFormattingNoThrow(node, node, data);
					text = HtmlUtils.objectToHtml(transformed);
					if (tooltipTrigger != TooltipTrigger.LINK && text.equals(getShortText(text)))
						return null;
				}
				catch (Exception e) {
					text = TextUtils.format("MainView.errorUpdateText", data, e.getLocalizedMessage());
				}
				if (!HtmlUtils.isHtml(text)) {
					text = HtmlUtils.plainToHTML(text);
				}
				final String tooltipText = htmlBodyStyle.isEmpty() ? text
						: text.replaceFirst("<body>",
						"<body><div style=\"" + htmlBodyStyle.toString() + "\">")
				    .replaceFirst("</body>", "</div></body>");
				return tooltipText;
			}

			private String getTooltipHtmlStyle(final ModeController modeController, NodeModel node,
			        MainView view) {
				final NodeStyleController style = modeController.getExtension(NodeStyleController.class);
				final Font font = style.getFont(node, StyleOption.FOR_UNSELECTED_NODE);
				float zoom = view.getNodeView().getMap().getZoom();
				final String htmlBodyStyle = new StringBuilder()
				    .append(new CssRuleBuilder().withHTMLFont(font)
				        .withColor(view.getUnselectedForeground())
				        .withBackground(view.getNodeView().getTextBackground())
				        .withAlignment(view.getHorizontalAlignment())
				        .withMaxWidthAsPt(zoom, style.getMaxWidth(node, StyleOption.FOR_UNSELECTED_NODE)))
				        .toString();
				return htmlBodyStyle;
			}
		});
	}

	public void setIsMinimized(NodeModel node, boolean shortened) {
		boolean oldState = ShortenedTextModel.getShortenedTextModel(node) != null;
		if (oldState == shortened) {
			return;
		}
		if (shortened) {
			ShortenedTextModel.createShortenedTextModel(node);
		}
		else {
			node.removeExtension(ShortenedTextModel.class);
		}
		Controller.getCurrentModeController().getMapController().nodeChanged(node, "SHORTENER", oldState, shortened);
	}

	public boolean parseData() {
		return false;
	}

	public String getNodeFormat(NodeModel node) {
		return modeController.getExtension(NodeStyleController.class).getNodeFormat(node);
	}

	public boolean getNodeNumbering(NodeModel node) {
		return nodeNumberingEnabled && modeController.getExtension(NodeStyleController.class).getNodeNumbering(node);
	}

	public ModeController getModeController() {
		return modeController;
	}

	public boolean canEdit() {
		return false;
	}

	public Hyperlink toLink(final Object value, final NodeModel node, Object extension) {
	    if(value instanceof Hyperlink)
	        return (Hyperlink) value;
	    final Object transformedObject = getTransformedObjectNoFormattingNoThrow(node, extension, value);
	    return modeController.getExtension(LinkController.class).toLink(node, transformedObject);
	}

}

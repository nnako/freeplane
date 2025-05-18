package org.freeplane.plugin.latex;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.JRestrictedSizeScrollPane;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.features.text.AbstractContentTransformer;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.TransformationException;
import org.freeplane.features.text.mindmapmode.EditNodeBase;
import org.freeplane.features.text.mindmapmode.EditNodeBase.IEditControl;
import org.freeplane.features.text.mindmapmode.EditNodeDialog;
import org.freeplane.features.text.mindmapmode.IEditBaseCreator;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.text.mindmapmode.SourceTextEditorUIConfigurator;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXIcon;

public class LatexRenderer extends AbstractContentTransformer implements IEditBaseCreator {

	private static final String LATEX_EDITOR_FONT_SIZE = "latex_editor_font_size";
	private static final String LATEX_EDITOR_FONT = "latex_editor_font";
	private static final String LATEX_EDITOR_DISABLE_INLINE = "latex_disable_editor";
	private static final String LATEX = "\\latex";
	private static final String UNPARSED_LATEX = "\\unparsedlatex";
	static final String LATEX_CONTENT_TYPE = "latex";
	static final String LATEX_FORMAT = "latexPatternFormat";
	static final String UNPARSED_LATEX_FORMAT = "unparsedLatexPatternFormat";


	public LatexRenderer() {
		super(20);
	}

	@Override
	public Object transformContent(NodeModel node,
			Object nodeProperty, Object content, TextController textController, Mode mode)
			throws TransformationException {
        if(mode == Mode.TEXT)
            return content;
		final String latext = getText(node, nodeProperty, content, Target.VIEW, textController);
		if (latext == null)
			return content;
		final NodeStyleController ncs = NodeStyleController.getController(textController.getModeController());
		int widthWithInsets = ncs.getMaxWidth(node, StyleOption.FOR_UNSELECTED_NODE).toBaseUnitsRounded();
		final int maxWidth = Math.max(0, widthWithInsets - 4);
		TeXText teXt = new TeXText(latext);
		int fontSize = Math.round(ncs.getFontSize(node, StyleOption.FOR_UNSELECTED_NODE) * UITools.FONT_SCALE_FACTOR);
		TeXIcon icon = teXt.createTeXIcon(TeXConstants.STYLE_DISPLAY, fontSize, TeXConstants.ALIGN_LEFT, maxWidth);
		int insetSize = (widthWithInsets - maxWidth) / 2;
		icon.setInsets(new Insets(insetSize, insetSize, insetSize, insetSize));
		return new LatexRenderIcon(icon);
	}

	private static enum Target { VIEW, EDITOR };

    @Override
    public EditNodeBase createEditor(final NodeModel node, Object nodeProperty,
            Object content, final EditNodeBase.IEditControl editControl, final boolean editLong) {
        JEditorPane textEditor = createTextEditorPane(this::createScrollPane, node, nodeProperty, content, ! editLong);
        return textEditor == null ? null :createEditor(node, editControl, textEditor);
    }

    private JRestrictedSizeScrollPane createScrollPane() {
        final JRestrictedSizeScrollPane scrollPane = new JRestrictedSizeScrollPane();
        UITools.setScrollbarIncrement(scrollPane);
        scrollPane.setMinimumSize(new Dimension(0, 60));
        return scrollPane;
    }


    @Override
    public JEditorPane createTextEditorPane(Supplier<JScrollPane> scrollPaneSupplier, final NodeModel node, Object nodeProperty,
            Object content, boolean editInline) {
		// this option has been added to work around bugs in JSyntaxPane with Chinese characters
		if (editInline && ResourceController.getResourceController().getBooleanProperty(LATEX_EDITOR_DISABLE_INLINE))
			return null;
        String latexText = getText(node, nodeProperty, content, Target.EDITOR, MTextController.getController());
        if(latexText == null)
            return null;

		JEditorPane textEditor = new JEditorPane();
		scrollPaneSupplier.get().setViewportView(textEditor);
		textEditor.setContentType("text/latex");
		textEditor.setText(latexText);
		SourceTextEditorUIConfigurator.configureColors(textEditor);
		final String fontName = ResourceController.getResourceController().getProperty(LATEX_EDITOR_FONT);
		final int fontSize = ResourceController.getResourceController().getIntProperty(LATEX_EDITOR_FONT_SIZE);
		final Font font = UITools.scaleUI(new Font(fontName, Font.PLAIN, fontSize));
		textEditor.setFont(font);
        return textEditor;
	}

    private EditNodeBase createEditor(NodeModel node, IEditControl editControl,
            JEditorPane textEditor) {
        final AWTEvent firstEvent = MTextController.getController().getEventQueue().getFirstEvent();
		final EditNodeDialog editNodeDialog = new EditNodeDialog(node, firstEvent, false, editControl, false, textEditor);
		editNodeDialog.setTitle(TextUtils.getText("latex_editor"));
		return editNodeDialog;
    }

	private String getText(NodeModel node, Object nodeProperty, Object content, Target targetMode, TextController textController) {
		if(! (content instanceof String))
			return null;
		MNoteController noteController = MNoteController.getController();
		String contentType;
		if (nodeProperty instanceof NodeModel) {
		    if (! textController.isTextFormattingDisabled(node)) {
		        contentType = textController.getNodeFormat(node);
		    } else
		        return  null;
		} else if (nodeProperty instanceof DetailModel) {
            String detailsContentType = textController.getDetailsContentType(node);
            if (LatexRenderer.LATEX_CONTENT_TYPE.equals(detailsContentType)) {
                contentType = LatexRenderer.LATEX_FORMAT;
            }
			else if (TextController.CONTENT_TYPE_AUTO.equals(detailsContentType)) {
			    contentType = TextController.CONTENT_TYPE_AUTO;
         }
			else
				return  null;
        } else if (nodeProperty instanceof NoteModel) {
            String noteContentType = noteController.getNoteContentType(node);
            if (LatexRenderer.LATEX_CONTENT_TYPE.equals(noteContentType)) {
                contentType = LatexRenderer.LATEX_FORMAT;
            }
			else if (TextController.CONTENT_TYPE_AUTO.equals(noteContentType)) {
			    contentType = TextController.CONTENT_TYPE_AUTO;
         }
			else
				return  null;
        } else
		    return  null;
		String plainOrHtmlText = (String) content;
		String text = HtmlUtils.htmlToPlain(plainOrHtmlText);
		String latexText = getLatexText(text, contentType, targetMode);
		return latexText;
	}

	private String getLatexText(final String nodeText, final String patternFormat, final Target mode)
	{
		boolean includePrefix = mode == Target.EDITOR;

		if(startsWithPrefix(nodeText, LATEX)){
			return includePrefix ? nodeText : nodeText.substring(LATEX.length() + 1);
		}
		else if(LatexRenderer.LATEX_FORMAT.equals(patternFormat)){
			return nodeText;
		} else if(startsWithPrefix(nodeText, UNPARSED_LATEX) && mode == Target.EDITOR) {
			return nodeText;
		} else if(LatexRenderer.UNPARSED_LATEX_FORMAT.equals(patternFormat) && mode == Target.EDITOR) {
			return nodeText;
		} else {
			return null;
		}
	}

    private static boolean startsWithPrefix(final String nodeText, final String prefix)
    {
        int startLength = prefix.length() + 1;
        return nodeText.length() > startLength && nodeText.startsWith(prefix) &&
             Character.isWhitespace(nodeText.charAt(startLength - 1));
    }


}

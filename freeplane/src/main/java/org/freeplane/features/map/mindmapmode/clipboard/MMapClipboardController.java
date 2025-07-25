/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
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
package org.freeplane.features.map.mindmapmode.clipboard;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.freeplane.api.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.CaseSensitiveFileNameExtensionFilter;
import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog;
import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog.MessageType;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.FileUtils;
import org.freeplane.core.util.HtmlProcessor;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.core.util.URIUtils;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.clipboard.ClipboardAccessor;
import org.freeplane.features.clipboard.mindmapmode.MClipboardController;
import org.freeplane.features.format.ScannerController;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.icon.mindmapmode.TagSelection;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.CloneEncryptedNodeException;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.MapReader;
import org.freeplane.features.map.MapReader.NodeTreeCreator;
import org.freeplane.features.map.MapWriter.Hint;
import org.freeplane.features.map.MapWriter.Mode;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.map.clipboard.MapClipboardController;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;
import org.freeplane.features.map.mindmapmode.InsertionRelation;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.SummaryGroupEdgeListAdder;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.n3.nanoxml.XMLException;
import org.freeplane.view.swing.features.filepreview.ImageAdder;
import org.freeplane.view.swing.features.filepreview.ViewerController;
import org.freeplane.view.swing.features.filepreview.ViewerController.PasteMode;

import com.lightdev.app.shtm.SHTMLDocument;
import com.lightdev.app.shtm.SHTMLEditorKit;
import com.lightdev.app.shtm.SHTMLWriter;

/**
 * @author Dimitry Polivaev
 */
public class MMapClipboardController extends MapClipboardController implements MClipboardController{
	public static final String RESOURCES_REMIND_USE_RICH_TEXT_IN_NEW_NODES = "remind_use_rich_text_in_new_nodes";
	private static final String FIND_EMAILS_IN_TEXT_PROPERTY = "findEmailsInText";
	private class DirectHtmlFlavorHandler implements IDataFlavorHandler {
		private final String textFromClipboard;

		public DirectHtmlFlavorHandler(final String textFromClipboard) {
			this.textFromClipboard = textFromClipboard;
		}

		void paste(final NodeModel target) {
			final String text = cleanHtml(textFromClipboard);
			MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
			final NodeModel node = mapController.newNode(text, Controller.getCurrentController().getMap());
			node.setSide(MapController.suggestNewChildSide(target, Side.DEFAULT));
            mapController.insertNode(node, target);
		}

		@Override
		public void paste(Transferable t, final NodeModel target, final Side side, int dropAction) {
			paste(target);
		}
	}

	private class FileListFlavorHandler implements IDataFlavorHandler {
		final ArrayList<File> fileList;

		public FileListFlavorHandler(final List<File> fileList) {
			super();
			this.fileList = new ArrayList<>(fileList);
		}

		@Override
		public void paste(Transferable t, final NodeModel target, final Side side, int dropAction) {
			boolean copyFile = dropAction == DnDConstants.ACTION_COPY;
	        final File mapFile = target.getMap().getFile();
			if ((copyFile || LinkController.getLinkType() == LinkController.LINK_RELATIVE_TO_MINDMAP) && mapFile == null) {
	        	JOptionPane.showMessageDialog(Controller.getCurrentController().getViewController().getCurrentRootComponent(),
	        	    TextUtils.getText("map_not_saved"), "Freeplane", JOptionPane.WARNING_MESSAGE);
	        	return;
	        }
			ViewerController viewerController = (Controller.getCurrentModeController().getExtension(ViewerController.class));
			boolean pasteImagesFromFiles = ResourceController.getResourceController().getBooleanProperty("pasteImagesFromFiles");
			Side newChildSide = side.isSibling() ? target.getSide() : side;
			final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
			if(! side.isSibling()  && mapController.placesNewChildFirst(target))
			    Collections.reverse(fileList);
			for (final File sourceFile : fileList) {
				final File file;
				if(copyFile){
					try {
						file = new TargetFileCreator().createTargetFile(mapFile, sourceFile.getName());
						file.getParentFile().mkdirs();
						FileUtils.copyFile(sourceFile, file);
					} catch (IOException e) {
						LogUtils.warn(e);
						continue;
					}
				}
				else
					file = sourceFile;
				if(! pasteImagesFromFiles || dropAction == DnDConstants.ACTION_LINK || !viewerController.paste(file, target, PasteMode.bySide(side))) {
					final NodeModel node = mapController.newNode(file.getName(), target.getMap());
					((MLinkController) LinkController.getController()).setLinkTypeDependantLink(node, file);
                    node.setSide(newChildSide);
					mapController.insertNode(node, target, InsertionRelation.bySide(side));
				}
			}
		}
	}

	interface IDataFlavorHandler {
		void paste(Transferable t, NodeModel target, Side side, int dropAction);
	}

    private class MindMapNodesFlavorHandler implements IDataFlavorHandler {
        private final String textFromClipboard;

        public MindMapNodesFlavorHandler(final String textFromClipboard) {
            this.textFromClipboard = textFromClipboard;
        }

        @Override
        public void paste(Transferable t, final NodeModel target, final Side side, int dropAction) {
            if (textFromClipboard != null) {
                paste(target, side);
            }
        }

        private void paste(NodeModel target, final Side side) {
            final ArrayList<String> textLines = new ArrayList<>(Arrays.asList(textFromClipboard.split(MapClipboardController.NODESEPARATOR)));
            final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
            final MapReader mapReader = mapController.getMapReader();
            if(! side.isSibling()  && mapController.placesNewChildFirst(target))
                Collections.reverse(textLines);
            synchronized(mapReader) {
                final NodeTreeCreator nodeTreeCreator = mapReader.nodeTreeCreator(target.getMap());
                nodeTreeCreator.setHint(Hint.MODE, Mode.CLIPBOARD);
                for (int i = 0; i < textLines.size(); ++i) {
                    try {
                        final NodeModel newModel = nodeTreeCreator.create(new StringReader(textLines.get(i)));
                        newModel.removeExtension(FreeNode.class);
                        newModel.setSide(side.isSibling() ? target.getSide() : side);
                        mapController.insertNode(newModel, target, InsertionRelation.bySide(side));
                        if(side == Side.AS_SIBLING_AFTER)
                        	target = newModel;
                    }
                    catch (final XMLException e) {
                        LogUtils.severe("error on paste", e);
                    }
                }
                nodeTreeCreator.finish(target);
            }
            mapController.balanceFirstGroupNodes(target);
        }
    }
    private class TagSelectionHandler implements IDataFlavorHandler {
        private final String textFromClipboard;

        public TagSelectionHandler(final String textFromClipboard) {
            this.textFromClipboard = textFromClipboard;
        }

        @Override
        public void paste(Transferable t, final NodeModel target, final Side side, int dropAction) {
            if (textFromClipboard != null) {
                Set<NodeModel> selection = Controller.getCurrentController().getSelection().getSelection();
                if(selection.contains(target))
                    selection.forEach(this::paste);
                else
                    paste(target);
            }
        }

        private void paste(final NodeModel target) {
            ((MIconController)IconController.getController()).addTagsFromSpec(target, textFromClipboard);
        }
    }



	private static class PasteHtmlWriter extends SHTMLWriter {
		private final Element element;

		public PasteHtmlWriter(final Writer writer, final Element element, final HTMLDocument doc, final int pos,
		                       final int len) {
			super(writer, doc, pos, len);
			this.element = getStandAloneElement(element);
		}

		@Override
		protected ElementIterator getElementIterator() {
			return new ElementIterator(element);
		}

		private Element getStandAloneElement(final Element element) {
			final String name = element.getName();
			if (name.equals("ul") || name.equals("ol") || name.equals("table") || name.equals("html")) {
				return element;
			}
			return getStandAloneElement(element.getParentElement());
		}

		@Override
		public void write() throws IOException, BadLocationException {
			if (element.getName().equals("html")) {
				super.write();
				return;
			}
			write("<html>");
			super.write();
			write("</html>");
		}
	}

	private static final Pattern ATTRIBUTE_REGEX = Pattern.compile("\\s*\\+\t(\\S[^\t]*)(?:\t(.*?))\\s*");
	private class StringFlavorHandler implements IDataFlavorHandler {
		private final String textFromClipboard;

		public StringFlavorHandler(final String textFromClipboard) {
			this.textFromClipboard = textFromClipboard;
		}

		@Override
		public void paste(Transferable t, final NodeModel target, final Side side, int dropAction) {
			final TextFragment[] textFragments = split(textFromClipboard);
			pasteStringWithoutRedisplay(textFragments, target, side);
		}

		private TextFragment[] split(final String textFromClipboard) {
			final LinkedList<TextFragment> textFragments = new LinkedList<TextFragment>();
			final String[] textLines = textFromClipboard.split("\n");
			for (int i = 0; i < textLines.length; ++i) {
				String text = textLines[i];
				final Matcher matcher = ATTRIBUTE_REGEX.matcher(text);
				if(matcher.matches()) {
					textFragments.add(new TextFragment(matcher.group(1), matcher.group(2), TextFragment.ATTRIBUTE_DEPTH));
				}
				else {
					text = text.replaceAll("\t", "        ");
					if (text.matches(" *")) {
						continue;
					}
					int depth = 0;
					while (depth < text.length() && text.charAt(depth) == ' ') {
						++depth;
					}
					final String visibleText = text.trim();
					final String link = findLink(text, false);
					if (!visibleText.equals("")) {
						textFragments.add(new TextFragment(visibleText, link, depth));
					}
				}
			}
			return textFragments.toArray(new TextFragment[textFragments.size()]);
		}
	}

    private class StructuredTextFromHtmlFlavorHandler extends StructuredHtmlFlavorHandler implements IDataFlavorHandler {

        public StructuredTextFromHtmlFlavorHandler(String textFromClipboard) {
            super(textFromClipboard);
        }
        protected TextFragment createNodeTextFragment(final int depth, final String string,
                final String link) {
            return new TextFragment(HtmlUtils.htmlToPlain(string, true, true, ""), link, depth);
        }


    }
	private class StructuredHtmlFlavorHandler implements IDataFlavorHandler {
		private final String textFromClipboard;

		public StructuredHtmlFlavorHandler(final String textFromClipboard) {
			this.textFromClipboard = textFromClipboard;
		}

		private void addFragment(final HTMLDocument doc, final Element element, final int depth, final int start,
		                           final int end, final LinkedList<TextFragment> htmlFragments)
		        throws BadLocationException, IOException {
			final String paragraphText = doc.getText(start, end - start).trim();
			if (paragraphText.length() > 0 || element.getName().equals("img")) {
				final StringWriter out = new StringWriter();
				new PasteHtmlWriter(out, element, doc, start, end - start).write();
				final String string = out.toString();
				if (!string.equals("")) {
					final String link = findLink(string, true);
					final TextFragment htmlFragment = createNodeTextFragment(depth, string, link);
					htmlFragments.add(htmlFragment);
				}
			}
		}

        protected TextFragment createNodeTextFragment(final int depth, final String string,
                final String link) {
            return new TextFragment(string, link, depth);
        }

		private Element getParentElement(final HTMLDocument doc) {
			final Element htmlRoot = doc.getDefaultRootElement();
			final Element bodyElement = htmlRoot.getElement(htmlRoot.getElementCount() - 1);
			Element parentCandidate = bodyElement;
			do {
				if (parentCandidate.getElementCount() > 1) {
					return parentCandidate;
				}
				parentCandidate = parentCandidate.getElement(0);
			} while (!(parentCandidate.isLeaf() || parentCandidate.getName().equalsIgnoreCase("p-implied")));
			return bodyElement;
		}

		private boolean isSeparateElement(final Element current) {
			return !current.isLeaf();
		}

		@Override
		public void paste(Transferable t, final NodeModel target, final Side side, int dropAction) {
			pasteHtmlWithoutRedisplay(textFromClipboard, target, side);
		}

		private void pasteHtmlWithoutRedisplay(final Object t, final NodeModel parent, final Side side) {
			final String textFromClipboard = (String) t;
			final String cleanedTextFromClipboard = cleanHtml(textFromClipboard);
			final TextFragment[] htmlFragments = split(cleanedTextFromClipboard);
			pasteStringWithoutRedisplay(htmlFragments, parent, side);
		}

		private void split(final HTMLDocument doc, final Element parent, final LinkedList<TextFragment> htmlFragments,
		                   int depth) throws BadLocationException, IOException {
			final int elementCount = parent.getElementCount();
			int headerDepth = 0;
			boolean headerFound = false;
			int start = -1;
			int end = -1;
			Element last = null;
			for (int i = 0; i < elementCount; i++) {
				final Element current = parent.getElement(i);
				final String name = current.getName();
				final Matcher matcher = HEADER_REGEX.matcher(name);
				if (matcher.matches()) {
					try {
						if (!headerFound) {
							depth--;
						}
						final int newHeaderDepth = Integer.parseInt(matcher.group(1));
						depth += newHeaderDepth - headerDepth;
						headerDepth = newHeaderDepth;
						headerFound = true;
					}
					catch (final NumberFormatException e) {
						LogUtils.severe(e);
					}
				}
				else {
					if (headerFound) {
						headerFound = false;
						depth++;
					}
				}
				final boolean separateElement = isSeparateElement(current);
				if (separateElement && current.getElementCount() != 0) {
					start = -1;
					last = null;
					split(doc, current, htmlFragments, depth + 1);
					continue;
				}
				if (separateElement && start != -1) {
					addFragment(doc, last, depth, start, end, htmlFragments);
				}
				if (start == -1 || separateElement) {
					start = current.getStartOffset();
					last = current;
				}
				end = current.getEndOffset();
				if (separateElement) {
					addFragment(doc, current, depth, start, end, htmlFragments);
				}
			}
			if (start != -1) {
				addFragment(doc, last, depth, start, end, htmlFragments);
			}
		}

		private TextFragment[] split(final String text) {
			final LinkedList<TextFragment> htmlFragments = new LinkedList<TextFragment>();
			final HTMLEditorKit kit = new SHTMLEditorKit();
			final HTMLDocument doc =  new SHTMLDocument();
			HtmlProcessor.configureUnknownTags(doc);
			final StringReader buf = new StringReader(text);
			try {
				kit.read(buf, doc, 0);
				final Element parent = getParentElement(doc);
				split(doc, parent, htmlFragments, 0);
			}
			catch (Exception e) {
				LogUtils.severe(e);
			}
			return htmlFragments.toArray(new TextFragment[htmlFragments.size()]);
		}
	}

	private static class TextFragment {
		final static int ATTRIBUTE_DEPTH = -2;
		String text;
		String link;
		int depth;

		public TextFragment(final String text, final String link, final int depth) {
			super();
			this.text = text;
			this.link = link;
			this.depth = depth;
		}

		boolean isAttribute() {
			return depth == ATTRIBUTE_DEPTH;
		}

		boolean isNode() {
			return ! isAttribute();
		}

		@Override
		public String toString() {
			return "TextFragment [" + text + (link != null ? " [" + link  +  "]": "")  + "," + depth + "]";
		}


	}

	private class ImageFlavorHandler implements IDataFlavorHandler {

		final private Image image;

		public ImageFlavorHandler(Image img) {
			super();
			image = img;
		}

        @Override
		public void paste(Transferable t, NodeModel target, Side side, int dropAction) {
			final ModeController modeController = Controller.getCurrentModeController();
			final MMapController mapController = (MMapController) modeController.getMapController();
            File mindmapFile = target.getMap().getFile();
            if(mindmapFile == null) {
            	UITools.errorMessage(TextUtils.getRawText("map_not_saved"));
            	return;
            }
            //file that we'll save to disk.
            File imageFile;
            try {
            	imageFile = new TargetFileCreator().createTargetFile(mindmapFile, ImageAdder.IMAGE_FORMAT);
    			imageFile.getParentFile().mkdirs();
            	String imgfilepath=imageFile.getAbsolutePath();
            	File tempFile = imageFile = new File(imgfilepath);
            	final JFileChooser fileChooser = UITools.newFileChooser(imageFile);
            	final CaseSensitiveFileNameExtensionFilter filter = new CaseSensitiveFileNameExtensionFilter();
            	filter.addExtension(ImageAdder.IMAGE_FORMAT);
            	fileChooser.setAcceptAllFileFilterUsed(false);
            	fileChooser.setFileFilter(filter);
            	fileChooser.setSelectedFile(imageFile);
            	int returnVal = fileChooser.showSaveDialog(UITools.getCurrentRootComponent());
            	if (returnVal != JFileChooser.APPROVE_OPTION) {
            		tempFile.delete();
            		return;
            	}
            	imageFile = fileChooser.getSelectedFile();
            	if(tempFile.exists() && ! imageFile.getAbsoluteFile().equals(tempFile)){
            		tempFile.delete();
            	}
            	if(imageFile.isDirectory())
            		return;
            	if(! FileUtils.getExtension(imageFile.getName()).equals(ImageAdder.IMAGE_FORMAT))
            		imageFile = new File(imageFile.getPath() + '.' + ImageAdder.IMAGE_FORMAT);
            	final NodeModel node = mapController.newNode(imageFile.getName(), target.getMap());
				node.setSide(side.isSibling() ? target.getSide() : side);
            	mapController.insertNode(node, target, InsertionRelation.bySide(side));
            	new ImageAdder(image, mapController, mindmapFile, imageFile).attachImageToNode(node);
            }
            catch (IOException e) {
            	e.printStackTrace();
            }
        }

    }
	private static final Pattern HEADER_REGEX = Pattern.compile("h(\\d)", Pattern.CASE_INSENSITIVE);
	private static final String RESOURCE_UNFOLD_ON_PASTE = "unfold_on_paste";
	public static final String RESOURCES_CUT_NODES_WITHOUT_QUESTION = "cut_nodes_without_question";
    private static final int AS_NEW_BRANCH = -1;

	public static String firstLetterCapitalized(final String text) {
		if (text == null || text.length() == 0) {
			return text;
		}
		return text.substring(0, 1).toUpperCase() + text.substring(1, text.length());
	}

	private List<NodeModel> newNodes;

	/**
	 * @param modeController
	 */
	public MMapClipboardController(MModeController modeController) {
		super(modeController);
		createActions();
	}

	private String cleanHtml(String content) {
		content = content.replaceFirst("(?i)(?s)<head>.*</head>", "")
		        .replaceFirst("(?i)(?s)^.*<html[^>]*>", "<html>")
		    .replaceFirst("(?i)(?s)<body [^>]*>", "<body>")
		    .replaceAll("(?i)(?s)<script.*?>.*?</script>", "")
		    .replaceAll("(?i)(?s)<meta.*?>", "")
		    .replaceAll("(?i)(?s)</?tbody.*?>", "")
		    .replaceAll("(?i)(?s)<!--.*?-->", "")
		    .replaceAll(
		        "(?i)(?s)</?o[^>]*>", "");
        if(! content.contains("<html>"))
            content = "<html>" + content;
        if(! content.contains("<body>"))
            content = content.replaceFirst("<html>", "<html><body>");
		if (ResourceController.getResourceController().getBooleanProperty("cut_out_pictures_when_pasting_html")) {
			String contentWithoutImages = content.replaceAll("(?i)(?s)<img[^>]*>", "");
			final boolean contentContainsOnlyImages = HtmlUtils.htmlToPlain(contentWithoutImages).trim().isEmpty();
			if(! contentContainsOnlyImages) {
				content = contentWithoutImages;
			}
		}
		content = HtmlUtils.unescapeHTMLUnicodeEntity(content);
		return content;
	}

	/**
	 * @param modeController
	 */
	private void createActions() {
		final ModeController modeController = Controller.getCurrentModeController();
		modeController.addAction(new SelectedPasteAction());
		modeController.addAction(new CloneAction());
		modeController.addAction(new MoveAction());
	}

	@Override
    public Transferable copy(IMapSelection selection) {
	    final List<NodeModel> collection = selection.getSortedSelection(true);
		final MindMapNodesSelection transferable = copy(new SummaryGroupEdgeListAdder(collection).addSummaryEdgeNodes());
		transferable.setNodeObjects(collection, false);
		return transferable;
    }



	@Override
	public Transferable copySingle(Collection<NodeModel> source) {
		final MindMapNodesSelection transferable = (MindMapNodesSelection) super.copySingle(source);
		transferable.setNodeObjects(new ArrayList<NodeModel>(source), true);
		return transferable;
	}

	private void cut(IMapSelection selection) {
		final List<NodeModel> collection = selection.getSortedSelection(true);
		final MindMapNodesSelection transferable = copy(new SummaryGroupEdgeListAdder(collection).addSummaryEdgeNodes(), CopiedNodeSet.ALL_NODES, CopiedNodeSet.ALL_NODES);
		((MMapController) Controller.getCurrentModeController().getMapController()).deleteNodes(collection);
		setClipboardContents(transferable);
	}

	private IDataFlavorHandler getFlavorHandler(final Transferable t) {
		if (t.isDataFlavorSupported(MindMapNodesSelection.mindMapNodesFlavor)) {
			try {
				final String textFromClipboard = t.getTransferData(MindMapNodesSelection.mindMapNodesFlavor).toString();
				return new MindMapNodesFlavorHandler(textFromClipboard);
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if(t.isDataFlavorSupported(TagSelection.tagFlavor)) {
	          try {
	                final String textFromClipboard = t.getTransferData(TagSelection.tagFlavor).toString();
	                return new TagSelectionHandler(textFromClipboard);
	            }
	            catch (final UnsupportedFlavorException e) {
	            }
	            catch (final IOException e) {
	            }

		}
		final ResourceController resourceController = ResourceController.getResourceController();
		DataFlavor supportedHtmlFlavor = getSupportedHtmlFlavor(t);
		if (supportedHtmlFlavor != null) {
			try {
				final String textFromClipboard = t.getTransferData(supportedHtmlFlavor).toString();
				if(textFromClipboard.isEmpty())
				    return null;
				if (textFromClipboard.charAt(0) != 65533) {
					if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
						final MTextController textController = (MTextController) TextController
						    .getController();
						final boolean richText = textController.useRichTextInEditor(RESOURCES_REMIND_USE_RICH_TEXT_IN_NEW_NODES);
						if (richText) {
							final boolean structuredHtmlImport = resourceController
							    .getBooleanProperty("structured_html_import");
							final IDataFlavorHandler htmlFlavorHandler;
							if (structuredHtmlImport) {
								htmlFlavorHandler = new StructuredHtmlFlavorHandler(textFromClipboard);
							}
							else {
								htmlFlavorHandler = new DirectHtmlFlavorHandler(textFromClipboard);
							}
							return htmlFlavorHandler;
						}
					}
				}
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if (t.isDataFlavorSupported(MindMapNodesSelection.fileListFlavor)) {
			try {
				final List<File> fileList = castToFileList(t.getTransferData(MindMapNodesSelection.fileListFlavor));
				if (!shouldIgnoreFileListFlavor(fileList))
					return new FileListFlavorHandler(fileList);
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				final String plainTextFromClipboard = t.getTransferData(DataFlavor.stringFlavor).toString();
				return new StringFlavorHandler(plainTextFromClipboard);
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			try {
				Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
				return new ImageFlavorHandler(image);
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		return null;
	}

	private boolean shouldIgnoreFileListFlavor(final List<File> fileList) {
		if(fileList == null || fileList.isEmpty())
			return true;
		final File file = fileList.get(0);
		if(file.isDirectory())
			return false;
	    final String name = file.getName();
		return name.endsWith(".URL") || name.endsWith(".url");
    }

	@SuppressWarnings("unchecked")
    private List<File> castToFileList(Object transferData) {
	    return (List<File>) transferData;
    }

	Collection<IDataFlavorHandler> getFlavorHandlers() {
		final Transferable t = getClipboardContents();
		final Collection<IDataFlavorHandler> handlerList = new LinkedList<IDataFlavorHandler>();
		if (t == null) {
			return handlerList;
		}
		if (t.isDataFlavorSupported(MindMapNodesSelection.mindMapNodesFlavor)) {
			try {
				final String textFromClipboard = t.getTransferData(MindMapNodesSelection.mindMapNodesFlavor).toString();
				handlerList.add(new MindMapNodesFlavorHandler(textFromClipboard));
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		DataFlavor supportedHtmlFlavor = getSupportedHtmlFlavor(t);
		if (supportedHtmlFlavor != null) {
			try {
				final String textFromClipboard = t.getTransferData(supportedHtmlFlavor).toString();
				if (textFromClipboard.length() > 0 && textFromClipboard.charAt(0) != 65533) {
					if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
						handlerList.add(new StructuredHtmlFlavorHandler(textFromClipboard));
						handlerList.add(new StructuredTextFromHtmlFlavorHandler(textFromClipboard));
						handlerList.add(new DirectHtmlFlavorHandler(textFromClipboard));
					}
				}
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				final String plainTextFromClipboard = t.getTransferData(DataFlavor.stringFlavor).toString();
				handlerList.add(new StringFlavorHandler(plainTextFromClipboard));
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if (t.isDataFlavorSupported(MindMapNodesSelection.fileListFlavor)) {
			try {
				final List<File> fileList = castToFileList(t.getTransferData(MindMapNodesSelection.fileListFlavor));
				if(fileList != null)
				    handlerList.add(new FileListFlavorHandler(fileList));
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			try {
				Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
				handlerList.add(new ImageFlavorHandler(image));
			}
			catch (final UnsupportedFlavorException e) {
			}
			catch (final IOException e) {
			}
		}
		return handlerList;
	}
	private DataFlavor getSupportedHtmlFlavor(Transferable t) {
		for (DataFlavor dataFlavor : t.getTransferDataFlavors())
			if(dataFlavor.getPrimaryType().equals(MindMapNodesSelection.htmlFlavor.getPrimaryType())
			&& dataFlavor.getSubType().equals(MindMapNodesSelection.htmlFlavor.getSubType())
			&& dataFlavor.getRepresentationClass().equals(MindMapNodesSelection.htmlFlavor.getRepresentationClass())
			)
				return dataFlavor;
		return null;
	}

	public void paste(final Transferable t, final NodeModel target, final Side side) {
		paste(t, target, side, DnDConstants.ACTION_NONE);
	}

	public void paste(final Transferable t, final NodeModel target, final Side side, int dropAction) {
		if (t == null) {
			return;
		}
//
//		DataFlavor[] fl = t.getTransferDataFlavors();
//		for (int i = 0; i < fl.length; i++) {
//			System.out.println(fl[i]);
//		}

		final IDataFlavorHandler handler = getFlavorHandler(t);
		paste(t, handler, target, side, dropAction);
	}

	void paste(final Transferable t, final IDataFlavorHandler handler, final NodeModel target, final Side side) {
		paste(t, handler, target, side, DnDConstants.ACTION_NONE);
    }

	void paste(final Transferable t, final IDataFlavorHandler handler, final NodeModel target, final Side side, int dropAction) {
		if (handler == null) {
			return;
		}
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		if (side.isSibling() && !mapController.isWriteable(target.getParentNode())
				|| ! side.isSibling() && !mapController.isWriteable(target)) {
			final String message = TextUtils.getText("node_is_write_protected");
			UITools.errorMessage(message);
			return;
		}
		Controller controller = Controller.getCurrentController();
        try {
			controller.getViewController().setWaitingCursor(true);
			if (newNodes == null) {
			    newNodes = new LinkedList<NodeModel>();
			}
			newNodes.clear();
			handler.paste(t, target, side, dropAction);
			if ( ! side.isSibling()) {
			    if (mapController.isFolded(target)) {
                    if (ResourceController.getResourceController().getBooleanProperty(RESOURCE_UNFOLD_ON_PASTE)) {
                        mapController.unfoldAndScroll(target, controller.getSelection().getFilter());
                    }
                }
			}
			for (final NodeModel child : newNodes) {
			    AttributeController.getController().performRegistrySubtreeAttributes(child);
			}
        }
		finally {
			controller.getViewController().setWaitingCursor(false);
		}
	}

	private void pasteStringWithoutRedisplay(final TextFragment[] textFragments, NodeModel target,
	                                              final Side side) {
		NodeModel parent;
		int insertionIndex;
		if (side.isSibling()) {
			final NodeModel childNode = target;
			parent = target.getParentNode();
			insertionIndex = parent.getIndex(childNode) + (side == Side.AS_SIBLING_BEFORE ? 0 : 1);
		}
		else{
			parent = target;
			insertionIndex = AS_NEW_BRANCH;
		}
		final ArrayList<NodeModel> parentNodes = new ArrayList<NodeModel>();
		final ArrayList<Integer> parentNodesDepths = new ArrayList<Integer>();
		parentNodes.add(parent);
		parentNodesDepths.add(new Integer(-1));
		for (int i = 0; i < textFragments.length; ++i) {
			final TextFragment textFragment = textFragments[i];
			if(textFragment.isNode()) {
				insertionIndex = addNode(parent, insertionIndex, parentNodes, parentNodesDepths,
					textFragment);
			}
			else if(textFragment.isAttribute()) {
				NodeModel node = parentNodes.get(parentNodes.size() - 1);
				addAttribute(node, textFragment, parent==node);
			}
		}
		insertNewNodes(parent, insertionIndex, side.isSibling() ? target.getSide() : side, parentNodes);
	}

	private void addAttribute(NodeModel node, final TextFragment textFragment, boolean toExistingNode) {
		final String name = textFragment.text;
		final Object value = ScannerController.getController().parse(textFragment.link);
		final Attribute atribute = new Attribute(name, value);
		if(toExistingNode) {
			MAttributeController.getController().addAttribute(node, atribute);
		}
		else {
			NodeAttributeTableModel attributes = node.getExtension(NodeAttributeTableModel.class);
			if(attributes == null) {
				attributes = new NodeAttributeTableModel();
				node.addExtension(attributes);
			}
			attributes.addRowNoUndo(node, atribute);
		}
	}

	private int addNode(NodeModel parent, int insertionIndex,
						final ArrayList<NodeModel> parentNodes, final ArrayList<Integer> parentNodesDepths,
						final TextFragment textFragment) {
		final MapModel map = parent.getMap();
		final NodeModel node = createNode(map, textFragment);
		return insertNode(parent, insertionIndex, parentNodes, parentNodesDepths, textFragment, node);
	}

	private int insertNode(NodeModel parent, int insertionIndex,
						   final ArrayList<NodeModel> parentNodes, final ArrayList<Integer> parentNodesDepths,
						   final TextFragment textFragment, final NodeModel node) {
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		if(insertionIndex == AS_NEW_BRANCH)
		    insertionIndex = mapController.findNewNodePosition(parent);
		for (int parentNodeIndex = parentNodes.size() - 1; parentNodeIndex >= 0; --parentNodeIndex) {
			if (textFragment.depth > parentNodesDepths.get(parentNodeIndex).intValue()) {
				int firstCompletedIndex = parentNodeIndex + 1;
				for (int completedParentNodeIndex = firstCompletedIndex;
						completedParentNodeIndex < parentNodes.size();
						completedParentNodeIndex++) {
					final NodeModel n = parentNodes.get(completedParentNodeIndex);
					if (n.getParentNode() == null) {
						mapController.insertNode(n, parent, insertionIndex++);
					}
				}
				parentNodes.subList(firstCompletedIndex,parentNodes.size()).clear();
				parentNodesDepths.subList(firstCompletedIndex,parentNodesDepths.size()).clear();
				final NodeModel target = parentNodes.get(parentNodeIndex);
				if (target != parent) {
					target.setFolded(true);
					target.insert(node, target.getChildCount());
				}
				parentNodes.add(node);
				parentNodesDepths.add(new Integer(textFragment.depth));
				break;
			}
		}
		return insertionIndex;
	}

	private NodeModel createNode(final MapModel map, final TextFragment textFragment) {
		String text = textFragment.text;
		final String link = textFragment.link;
		URI uri = null;
		if (link != null) {
			try {
				URI linkUri = URIUtils.createURIFromString(link);
				uri = linkUri;

				File absoluteFile = UrlManager.getController().getAbsoluteFile(map, uri);
				if(absoluteFile != null) {
					//if ("file".equals(linkUri.getScheme())) {
					final File mapFile = map.getFile();
					uri  = LinkController.toLinkTypeDependantURI(mapFile, absoluteFile);
					if(link.equals(text)){
						text =  uri.toString();
					}
				}

			}
			catch (Exception e) {
			}
		}
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		final NodeModel node = mapController.newNode(text, map);
		if(uri != null){
			NodeLinks.createLinkExtension(node).setHyperLink(new Hyperlink(uri));
		}
		return node;
	}

	private void insertNewNodes(NodeModel parent, int insertionIndex, Side side,
								final ArrayList<NodeModel> parentNodes) {
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		final MapModel map = parent.getMap();
		for (int k = 0; k < parentNodes.size(); ++k) {
			final NodeModel node = parentNodes.get(k);
			if (map.getRootNode() != node && node.getParentNode() == null) {
				node.setSide(side);
				mapController.insertNode(node, parent, insertionIndex++);
			}
		}
	}

	private enum Operation{CLONE, MOVE};

	public void addClone(final Transferable transferable, final NodeModel target) {
		processTransferable(transferable, target, Operation.CLONE);
	}

	public void move(final Transferable transferable, final NodeModel target) {
		processTransferable(transferable, target, Operation.MOVE);
	}

	@SuppressWarnings("unchecked")
	private void processTransferable(final Transferable transferable, final NodeModel target, Operation operation) {
		try {
			final Collection<NodeModel> clonedNodes;
			final boolean asSingleNodes;
			if (operation == Operation.CLONE && transferable.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeSingleObjectsFlavor)){
				clonedNodes = (Collection<NodeModel>) transferable.getTransferData(MindMapNodesSelection.mindMapNodeSingleObjectsFlavor);
				asSingleNodes = true;
			}
			else if(transferable.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)){
				clonedNodes = (Collection<NodeModel>) transferable.getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
				asSingleNodes = false;
			}
			else
				return;

			final List<NodeModel> movedNodes = new ArrayList<NodeModel>(clonedNodes.size());
			final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
			int newNodePosition = mapController.findNewNodePosition(target);
			for(NodeModel clonedNode:clonedNodes){
				if(! asSingleNodes && clonedNode.getParentNode() == null || ! clonedNode.getMap().equals(target.getMap()))
					return;
				if (asSingleNodes || (!clonedNode.isRoot() && ! clonedNode.subtreeContainsCloneOf(target))) {
					switch(operation){
					case CLONE:
						try {
							final NodeModel clone = asSingleNodes ? clonedNode.cloneContent() : clonedNode.cloneTree();
                            mapController.addNewNode(clone, target, newNodePosition++);
						} catch (CloneEncryptedNodeException e) {
							UITools.errorMessage(TextUtils.getText("can_not_clone_encrypted_node"));
						}
						break;
					case MOVE:
						movedNodes.add(clonedNode);
						break;
					}
				}
			}
			switch(operation){
			case MOVE:
				mapController.moveNodes(movedNodes, target, InsertionRelation.AS_CHILD);
					break;
			default:
				break;
			}
		}
		catch (Exception e) {
	        LogUtils.severe(e);
        }
    }

	public Transferable getClipboardContents() {
		return ClipboardAccessor.getInstance().getClipboardContents();
	}

	@Override
	public boolean canCut() {
		return true;
	}

	@Override
	public void cut() {
		final Controller controller = Controller.getCurrentController();
		final NodeModel root = controller.getMap().getRootNode();
		IMapSelection selection = controller.getSelection();
		if (selection.isSelected(root)) {
			UITools.errorMessage(TextUtils.getText("cannot_delete_root"));
			return;
		}
		final int showResult = OptionalDontShowMeAgainDialog.show("really_cut_node",
		    MMapClipboardController.RESOURCES_CUT_NODES_WITHOUT_QUESTION,
		    MessageType.ONLY_OK_SELECTION_IS_STORED);
		if (showResult != JOptionPane.OK_OPTION) {
			return;
		}
		cut(selection);
		controller.getMapViewManager().obtainFocusForSelected();

	}

	@Override
	public boolean canPaste(Transferable t) {
		return true;
	}

	@Override
	public void paste(ActionEvent event, Transferable t) {
		paste(t);
	}

    public void paste(Transferable t) {
        final NodeModel parent = Controller.getCurrentController().getSelection().getSelected();
		if(parent != null)
		    paste(t, parent);
    }

    public void paste(Transferable t, final NodeModel parent) {
        paste(t, parent, MapController.suggestNewChildSide(parent, Side.DEFAULT));
    }

    private String findLink(final String string, boolean isHtml) {
        boolean findsEmailsInText = ResourceController.getResourceController().getBooleanProperty(FIND_EMAILS_IN_TEXT_PROPERTY);
        return LinkController.findLink(string, isHtml, findsEmailsInText);
    }
}

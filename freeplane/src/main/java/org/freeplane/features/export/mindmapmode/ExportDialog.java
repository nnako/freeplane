/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Eric Lavarde, Freeplane admins
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
package org.freeplane.features.export.mindmapmode;

import java.awt.Component;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.freeplane.api.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.CaseSensitiveFileNameExtensionFilter;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.FileUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;

/**
 * This class uses the JFileChooser dialog to allow users to choose a file name to
 * export to. The filter selection is created by gathering all the *.xsl files
 * present in the user-specific and system-specific Export-directories of Freeplane.
 * Those files are recognised by their extension (.xsl) but also by the fact that they
 * contain within the 5 first lines a string of the form:
 * <pre>MINDMAPEXPORT <i>extensions</i> <i>description</i></pre>
 * where the fields in italic are relative to the file format to which the mindmap will
 * be exported to using this specific XSLT sheet:
 * <ul>
 * <li><i>extensions</i> is a semi-column separated list of acceptable file extensions
 * without asterisk or dot, e.g. "jpg;jpeg".</li>
 * <li><i>description</i> is a description of the file format, e.g. "JPEG image".</li>
 * </ul>
 * Only the first unique combination of extensions and description will be kept, in such
 * a way that users can "overwrite" an already existing XSLT sheet with their own
 * version.
 * @author Eric Lavarde
 * @see javax.swing.JFileChooser
 *
 */
public class ExportDialog {
	private static final String LAST_CHOOSEN_EXPORT_FILE_FILTER = "lastChoosenExportFileFilter";
	public static final String EXPORT_MAP_TITLE = "ExportAction.text";
	public static final String EXPORT_BRANCHES_TITLE = "ExportBranchesAction.text";
	/** the JFileChooser dialog used to choose filter and the file to export to. */
	final private JFileChooser fileChooser = UITools.newFileChooser(null);
	final private Map<FileFilter, IExportEngine> exportEngines;

	/**
	 * This constructor does <i>not</i> the export per itself.
	 * It populates the {@link #fileChooser} field
	 * (especially the {@link JFileChooser#getChoosableFileFilters() choosable
	 * file filters}).
	 */
	ExportDialog(List<FileFilter> fileFilters, Map<FileFilter, IExportEngine> exportEngines, String dialogTitle) {
		super();
		this.exportEngines = exportEngines;
		fileChooser.setAcceptAllFileFilterUsed(false); // the user can't select an "All Files filter"
		fileChooser.setDialogTitle(TextUtils.getText(dialogTitle));
		fileChooser.setToolTipText(TextUtils.getText("select_file_export_to"));
		for (FileFilter filter : fileFilters) {
	        fileChooser.addChoosableFileFilter(filter);
        }
		preselectFileFilter();
	}

	private void preselectFileFilter() {
		final String lastChoosenExportFileFilter = ResourceController.getResourceController().getProperty(LAST_CHOOSEN_EXPORT_FILE_FILTER, null);
		final FileFilter[] choosableFileFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter f: choosableFileFilters){
			if(f.getDescription().equals(lastChoosenExportFileFilter)) {
				fileChooser.setFileFilter(f);
				return;
			}
		}
		final FileFilter fileFilter = choosableFileFilters[0];
		fileChooser.setFileFilter(fileFilter);
	}

	void export(final Component parentframe, List<NodeModel> branches) {
		if (exportEngines.isEmpty()) {
			JOptionPane.showMessageDialog(parentframe, TextUtils.getText("xslt_export_not_possible"));
			return;
		}
		// Finish to setup the File Chooser...
		// And then use it
		final String absolutePathWithoutExtension;
		MapModel map = branches.get(0).getMap();
		final File xmlSourceFile = map.getFile();
		if (xmlSourceFile != null) {
			absolutePathWithoutExtension = FileUtils.removeExtension(xmlSourceFile.getAbsolutePath());
		}
		else {
			absolutePathWithoutExtension = null;
		}
		final PropertyChangeListener filterChangeListener = new PropertyChangeListener() {
			final private File selectedFile = absolutePathWithoutExtension == null ? null : new File(
			    absolutePathWithoutExtension);

			@Override
			public void propertyChange(final PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
					final FileFilter filter = fileChooser.getFileFilter();
					if(! (filter instanceof CaseSensitiveFileNameExtensionFilter)){
						return;
					}
					final File acceptableFile = getAcceptableFile(selectedFile, (CaseSensitiveFileNameExtensionFilter) filter);
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							fileChooser.setSelectedFile(acceptableFile);
						}
					});
					return;
				}
				if (selectedFile != null && evt.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
						final FileFilter filter = fileChooser.getFileFilter();
						if(! (filter instanceof CaseSensitiveFileNameExtensionFilter)){
							return;
						}
						final File acceptableFile = getAcceptableFile(selectedFile, (CaseSensitiveFileNameExtensionFilter) filter);
						final File currentDirectory = fileChooser.getCurrentDirectory();
						if(currentDirectory != null){
							final File file = new File (currentDirectory, acceptableFile.getName());
							fileChooser.setSelectedFile(file);
						}
						else
							fileChooser.setSelectedFile(acceptableFile);
					return;
				}
			}
		};
		filterChangeListener.propertyChange(new PropertyChangeEvent(fileChooser,
		    JFileChooser.FILE_FILTER_CHANGED_PROPERTY, null, fileChooser.getFileFilter()));
		try {
			fileChooser.addPropertyChangeListener(filterChangeListener);
			final int returnVal = fileChooser.showSaveDialog(parentframe);
			final FileFilter currentfileFilter = fileChooser.getFileFilter();
			final String lastFileFilterDescription = currentfileFilter.getDescription();
			if(currentfileFilter instanceof CaseSensitiveFileNameExtensionFilter)
				ResourceController.getResourceController().setProperty(
					LAST_CHOOSEN_EXPORT_FILE_FILTER, lastFileFilterDescription);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				// we check which filter has been selected by the user and use its
				// description as key for the map to get the corresponding XSLT file
				if(! (currentfileFilter instanceof CaseSensitiveFileNameExtensionFilter)){
					UITools.errorMessage(TextUtils.getText("invalid_export_file"));
					return;
				}
				final CaseSensitiveFileNameExtensionFilter fileFilter = (CaseSensitiveFileNameExtensionFilter) currentfileFilter;
				final File selectedFile = getAcceptableFile(fileChooser.getSelectedFile(), fileFilter);
				if (selectedFile == null) {
					return;
				}
				if (selectedFile.isDirectory()) {
					return;
				}
				if (selectedFile.exists()) {
					final String overwriteText = MessageFormat.format(TextUtils.getText("file_already_exists"),
					    new Object[] { selectedFile.toString() });
					final int overwriteMap = JOptionPane.showConfirmDialog(UITools.getCurrentRootComponent(), overwriteText,
					    overwriteText, JOptionPane.YES_NO_OPTION);
					if (overwriteMap != JOptionPane.YES_OPTION) {
						return;
					}
				}
				final IExportEngine exportEngine = exportEngines.get(fileFilter);
				exportEngine.export(branches, selectedFile);
			}
		}
		finally {
			fileChooser.removePropertyChangeListener(filterChangeListener);
		}
	}

	private File getAcceptableFile(File selectedFile, final CaseSensitiveFileNameExtensionFilter fileFilter) {
		if (selectedFile == null) {
			return null;
		}
		if (!fileFilter.accept(selectedFile)) {
			selectedFile = new File(selectedFile.getAbsolutePath() + '.' + fileFilter.getExtensionProposal());
		}
		return selectedFile;
	}
}

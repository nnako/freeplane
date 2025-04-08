/*
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
package org.freeplane.features.icon.mindmapmode;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.icon.TagCategories;

class TagTreeViewerFactory implements IExtension {

    @SuppressWarnings("serial")
    private class TreeTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if(c != tree)
                throw new IllegalArgumentException("Unexpected argument " + c);
            return tree.createTransferable();
        }

     }
    private final JTagTree tree;

    TagTreeViewerFactory(TagCategories tagCategories, Font tagFont) {
        tree = new JTagTree(tagCategories, tagFont);
        tree.setTransferHandler(new TreeTransferHandler());
        if(! GraphicsEnvironment.isHeadless()) {
            tree.setEditable(false);
            configureKeyBindings();
        }
    }

    private void configureKeyBindings() {
        ActionMap am = tree.getActionMap();

         @SuppressWarnings("serial")
        AbstractAction copyNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyNodes();
            }
        };
        am.put(TransferHandler.getCopyAction().getValue(Action.NAME), copyNodeAction);

    }


    boolean copyNodes() {
        TagCategorySelection t = tree.createTransferable();
        if(t  != null) {
            Clipboard clipboard = ClipboardAccessor.getSystemClipboard();
            clipboard.setContents(t, null);
            return true;
        }
        return false;
    }

    JTagTree getTree() {
        return tree;
    }
}

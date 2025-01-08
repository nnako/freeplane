/*
 * Created on 8 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
public class TrashBin {
    public final static TrashBin INSTANCE = new TrashBin();
    private static final Icon ICON = ResourceController.getResourceController().getImageIcon("trash.icon");

    private TrashBin() {/**/}

    public DragSourceListener showTrashBin(DragGestureEvent e, Runnable onDrop) {
        JLabel trashBin = new JLabel(ICON);
        trashBin.setHorizontalAlignment(SwingConstants.CENTER);
        trashBin.setBorder(BorderFactory.createLineBorder(trashBin.getForeground()));
        trashBin.setOpaque(true);

        Component draggable = e.getComponent();
        JWindow window = new JWindow();
        window.add(trashBin);
        window.setSize(trashBin.getPreferredSize());
        Point draggableLocation = draggable.getLocationOnScreen();
        window.setLocation(draggableLocation.x + draggable.getWidth() - window.getWidth(), draggableLocation.y);
        window.setVisible(true);

        @SuppressWarnings("unused")
 		DropTarget dropTarget = new DropTarget(window, new DropTargetAdapter() {
             @Override
             public void drop(DropTargetDropEvent dtde) {
            	 dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                 dtde.dropComplete(true);
                 SwingUtilities.invokeLater(onDrop::run);
             }
         });

        return new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent dsde) {
                SwingUtilities.invokeLater(window::dispose);
            }
        };
    }
}
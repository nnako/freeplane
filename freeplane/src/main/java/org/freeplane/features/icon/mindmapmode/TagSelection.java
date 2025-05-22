/*
 * Created on 11 May 2024
 *
 * author dimitry
 */
package org.freeplane.features.icon.mindmapmode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import org.freeplane.features.map.clipboard.MindMapNodesSelection;

public class TagSelection implements Transferable {
    public static final DataFlavor tagFlavor = new DataFlavor("application/x-freeplane-tag; class=java.lang.String", "Freeplane Tags");
    public static final DataFlavor uuidFlavor = new DataFlavor("application/x-freeplane-uuid; class=java.lang.String", "Freeplane UUID");
    public static final DataFlavor dropCopyActionFlavor = MindMapNodesSelection.dropCopyActionFlavor;

    private static final DataFlavor[] flavors = {
            tagFlavor,
            uuidFlavor,
            DataFlavor.stringFlavor,
            dropCopyActionFlavor,
        };

    private final String id;
    private final String tagSelection;
	private int dropAction;

    public TagSelection(UUID uuid, String tagData) {
        this.id = uuid.toString();
        tagSelection = tagData;
        dropAction = DnDConstants.ACTION_MOVE;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (flavor.equals(dropCopyActionFlavor))
            return dropAction == DnDConstants.ACTION_COPY;
        else
            return Stream.of(flavors).anyMatch(flavor::equals);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
            IOException {
        if(flavor.equals(uuidFlavor))
            return id;
        else if(flavor.equals(dropCopyActionFlavor))
            return dropAction;
        else
            return tagSelection;
    }

	public void setDropAction(int dropAction) {
		this.dropAction = dropAction;
	}
}

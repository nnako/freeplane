/*
 * Created on 11 May 2024
 *
 * author dimitry
 */
package org.freeplane.features.icon.mindmapmode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.stream.Stream;

public class TagCategorySelection implements Transferable {

    public static final DataFlavor tagCategoryFlavor = new DataFlavor("application/x-freeplane-tag-category; class=java.lang.String", "Freeplane Tag Categories");
    public static final DataFlavor stringFlavor = DataFlavor.stringFlavor;
    private static final DataFlavor tagFlavor = TagSelection.tagFlavor;
    private static final DataFlavor uuidFlavor = TagSelection.uuidFlavor;

    private static final DataFlavor[] flavors = {
            tagCategoryFlavor,
            tagFlavor,
            uuidFlavor,
            stringFlavor
        };
    private final String id;
    private final String tagCategorySelection;
    private final String tagSelection;
    public TagCategorySelection(String id, String tagCategoryData, String tagData) {
        this.id = id;
        tagCategorySelection = tagCategoryData;
        tagSelection = tagData;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Stream.of(flavors).anyMatch(flavor::equals);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
            IOException {
        if(flavor.equals(tagFlavor))
            return tagSelection;
        else if(flavor.equals(uuidFlavor))
            return id;
        else
            return tagCategorySelection;
    }

}

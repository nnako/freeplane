/*
 * Created on 14 Feb 2025
 *
 * author dimitry
 */
package org.freeplane.features.icon.mindmapmode;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

class ClipboardAccessor{
private static final Clipboard CLIPBOARD = GraphicsEnvironment.isHeadless() ? new Clipboard("") : null;
    public static Clipboard getSystemClipboard() {
        if(GraphicsEnvironment.isHeadless())
            return CLIPBOARD;
        else
            return Toolkit.getDefaultToolkit().getSystemClipboard();

    }
}
/*
 * Created on 31 Dec 2024
 *
 * author dimitry
 */
package org.freeplane.view.swing.ui;

public class MouseEventActor {

    public static final MouseEventActor INSTANCE = new MouseEventActor();
    private boolean isActive = false;
    public boolean isActive() {
        return isActive;
    }
    public void withMouseEvent(Runnable runnable) {
        boolean wasActive = isActive;
        this.isActive = true;
        try {
            runnable.run();
        }
        finally {
            isActive = wasActive;
        }
    }


}

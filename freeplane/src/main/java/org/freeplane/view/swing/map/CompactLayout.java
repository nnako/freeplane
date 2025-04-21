/*
 * Created on 21 Apr 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;

public enum CompactLayout {AVOID, ALLOW, FORCE;
    boolean isCompact() {
    	return this != AVOID;
    }
}
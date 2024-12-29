/*
 * Created on 19 Oct 2024
 *
 * author dimitry
 */
package org.freeplane.view.swing.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.freeplane.view.swing.map.MapView;

class RectangleMemorizer extends MouseAdapter {
    private Point startPoint;
    private MapView mapView;

    public RectangleMemorizer() {
    }


    @Override
    public void mousePressed(MouseEvent e) {
        Component component = e.getComponent();
        if(component instanceof MapView) {
            startPoint = e.getPoint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (startPoint == null)
            return;
        if(mapView != null) {
        	e.consume();
	        mapView.selectNodeViewBySelectionRectangle(! e.isShiftDown());
	        mapView.setSelectionRectangle(null);
	        mapView = null;
        }
        startPoint = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (startPoint == null)
            return;
        Component component = e.getComponent();
        if(! (component instanceof MapView))
        	return;
        mapView = (MapView) component;
        e.consume();
        Point endPoint = e.getPoint();
        Rectangle newRectangle = new Rectangle(
                Math.min(startPoint.x, endPoint.x),
                Math.min(startPoint.y, endPoint.y),
                Math.abs(startPoint.x - endPoint.x),
                Math.abs(startPoint.y - endPoint.y)
        );

        mapView.setSelectionRectangle(newRectangle);
    }

}

/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
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
package org.freeplane.view.swing.map.edge;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;

import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a sharp Edge of a MindMap.
 */
public class SharpLinearEdgeView extends SharpEdgeView {
	public SharpLinearEdgeView(NodeView source, NodeView target, Component paintedComponent) {
	    super(source, target, paintedComponent);
    }

	@Override
	public Stroke getStroke() {
		return getStroke(0);
	}

	@Override
	protected void draw(final Graphics2D g) {
        final int deltaX = getDeltaX();
        final int deltaY = getDeltaY();
        if(start != shapeStart) {
        	final int xs[] = { start.x, shapeStart.x + deltaX, end.x, shapeStart.x - deltaX};
        	final int ys[] = { start.y, shapeStart.y + deltaY, end.y, shapeStart.y - deltaY};
        	g.fillPolygon(xs, ys, 4);
        } else {
        	final int xs[] = { shapeStart.x + deltaX, end.x, shapeStart.x - deltaX};
        	final int ys[] = { shapeStart.y + deltaY, end.y, shapeStart.y - deltaY };
        	g.fillPolygon(xs, ys, 3);
        }
	}

	@Override
	public boolean detectCollision(final Point p) {
		final int w = getMap().getZoomed(getWidth() / 2 + 1);
		final Polygon polygon;
		if(start != shapeStart) {
        	final int xs[] = { start.x, shapeStart.x, end.x, shapeStart.x, start.x};
        	final int ys[] = { start.y, shapeStart.y, end.y, shapeStart.y, start.y};
			polygon = new Polygon(xs, ys, 5);
		}
		else {
			final int xs[] = { start.x, end.x, start.x };
			final int ys[] = { start.y + w, end.y, start.y - w };
			polygon = new Polygon(xs, ys, 3);
		}
		return new CollisionDetector().detectCollision(p, polygon);
	}
}

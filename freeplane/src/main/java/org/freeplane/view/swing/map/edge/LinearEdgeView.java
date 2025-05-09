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
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a single Edge of a MindMap.
 */
public class LinearEdgeView extends EdgeView {
	public LinearEdgeView(NodeView source, NodeView target, Component paintedComponent) {
	    super(source, target, paintedComponent);
    }

	@Override
	protected void draw(final Graphics2D g) {
		final int w = getWidth();
		if (w <= 1) {
			g.drawLine(start.x, start.y, shapeStart.x, shapeStart.y);
			g.drawLine(shapeStart.x, shapeStart.y, end.x, end.y);
			if (drawHiddenParentEdge()) {
				g.setColor(g.getBackground());
				g.setStroke(EdgeView.getEclipsedStroke());
				g.drawLine(start.x, start.y, shapeStart.x, shapeStart.y);
				g.drawLine(shapeStart.x, shapeStart.y, end.x, end.y);
			}
		}
		else {
	        final Point startControlPoint = getControlPoint(getStartConnectorLocation());
	        final int zoomedXCTRL = w + 1;
	        final int xctrl = startControlPoint.x * zoomedXCTRL;
	        final int yctrl = startControlPoint.y * zoomedXCTRL;
	        final Point endControlPoint = getControlPoint(getEndConnectorLocation());
	        final int childXctrl = endControlPoint.x * zoomedXCTRL;
	        final int childYctrl = endControlPoint.y * zoomedXCTRL;
			final int xs[] = { start.x, shapeStart.x, shapeStart.x + xctrl, end.x + childXctrl, end.x };
			final int ys[] = { start.y, shapeStart.y, shapeStart.y + yctrl, end.y + childYctrl, end.y };
			g.drawPolyline(xs, ys, 4);
			if (drawHiddenParentEdge()) {
				g.setColor(g.getBackground());
				g.setStroke(EdgeView.getEclipsedStroke());
				g.drawPolyline(xs, ys, 4);
			}
		}
	}

	@Override
	public boolean detectCollision(final Point p) {
		final Path2D line = new Path2D.Float();
		line.moveTo(start.x, start.y);
		if(start != shapeStart)
			line.lineTo(shapeStart.x, shapeStart.y);
		line.lineTo(end.x, end.y);
		return new CollisionDetector().detectCollision(p, line);
	}
}

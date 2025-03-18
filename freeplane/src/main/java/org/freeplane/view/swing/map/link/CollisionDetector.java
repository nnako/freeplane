/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry
 *
 *  This file author is Dimitry
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
package org.freeplane.view.swing.map.link;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

/**
 * @author Dimitry Polivaev
 * 09.08.2009
 */
public class CollisionDetector {
	/** MAXIMAL_RECTANGLE_SIZE_FOR_COLLISION_DETECTION describes itself. */
	static final private int MAXIMAL_RECTANGLE_SIZE_FOR_COLLISION_DETECTION = 16;

	public boolean detectCollision(final Point p, final Shape shape) {
		final PathIterator pathIterator = shape.getPathIterator(new AffineTransform(),
		    MAXIMAL_RECTANGLE_SIZE_FOR_COLLISION_DETECTION / 4);

		double lastCoords[] = new double[6];
		pathIterator.currentSegment(lastCoords);
		double threshold = MAXIMAL_RECTANGLE_SIZE_FOR_COLLISION_DETECTION / 2.0;

		for (;;) {
			pathIterator.next();
			final double nextCoords[] = new double[6];
			if (pathIterator.isDone() || PathIterator.SEG_CLOSE == pathIterator.currentSegment(nextCoords)) {
				break;
			}

			// Calculate distance from point to line segment
			double distance = distanceToLineSegment(
				lastCoords[0], lastCoords[1],
				nextCoords[0], nextCoords[1],
				p.getX(), p.getY());

			if (distance <= threshold) {
				return true;
			}

			lastCoords = nextCoords;
		}
		return false;
	}

	/**
	 * Calculates the shortest distance from a point to a line segment.
	 *
	 * @param x1 first point x of line segment
	 * @param y1 first point y of line segment
	 * @param x2 second point x of line segment
	 * @param y2 second point y of line segment
	 * @param px point x
	 * @param py point y
	 * @return distance from point to line segment
	 */
	private double distanceToLineSegment(double x1, double y1, double x2, double y2, double px, double py) {
		double A = px - x1;
		double B = py - y1;
		double C = x2 - x1;
		double D = y2 - y1;

		double dot = A * C + B * D;
		double len_sq = C * C + D * D;
		double param = -1;

		if (len_sq != 0) // in case of 0 length line
			param = dot / len_sq;

		double xx, yy;

		if (param < 0) {
			xx = x1;
			yy = y1;
		} else if (param > 1) {
			xx = x2;
			yy = y2;
		} else {
			xx = x1 + param * C;
			yy = y1 + param * D;
		}

		double dx = px - xx;
		double dy = py - yy;
		return Math.sqrt(dx * dx + dy * dy);
	}
}

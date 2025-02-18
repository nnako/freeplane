/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General License for more details.
 *
 *  You should have received a copy of the GNU General License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.map;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Polygon;

import org.freeplane.api.Dash;
import org.freeplane.features.nodestyle.NodeGeometryModel;

abstract class VariableInsetsPainter extends ShapedPainter {
	enum PaintOperation {
		DRAW(1), FILL(2);

		final int borderWidthFactor;

		private PaintOperation(int borderWidthFactor) {
			this.borderWidthFactor = borderWidthFactor;
		}

	}

	private double zoomedVerticalInset;
	private double zoomedHorizontalInset;

	VariableInsetsPainter(MainView mainView, NodeGeometryModel shapeConfiguration) {
		super(mainView, shapeConfiguration);
        zoomedVerticalInset = zoomedHorizontalInset = 0;
	}

	@Override
	boolean areInsetsFixed() {
		return false;
	}


	@Override
	Dimension getPreferredSize() {
		if (mainView.isPreferredSizeSet()) {
			return super.getPreferredSize();
		}
		final Dimension prefSize = getPreferredRectangleSizeWithoutMargin(mainView.getMaximumWidth());
		final double widthWithMargin = Math.max(prefSize.width*getHorizontalMarginFactor(), prefSize.width + getMinimumHorizontalInset());
		prefSize.width =  mainView.limitWidth((int) Math.ceil(widthWithMargin), mainView.getPaintedBorderWidth());
		prefSize.height = (int) Math.ceil(Math.max(prefSize.height *getVerticalMarginFactor(), prefSize.height + getMinimumVerticalInset()));
		return prefSize;
	}

	abstract double getVerticalMarginFactor() ;

	abstract double getHorizontalMarginFactor();

	double getMinimumHorizontalInset(){
		return ((int)getShapeConfiguration().getHorizontalMargin().toBaseUnits()-1) * mainView.getZoom() + mainView.getPaintedBorderWidth();
	}

	double getMinimumVerticalInset(){
		return ((int)getShapeConfiguration().getVerticalMargin().toBaseUnits()-1) * mainView.getZoom()+ mainView.getPaintedBorderWidth() - 1;
	}

	Dimension getPreferredRectangleSizeWithoutMargin(int maximumWidth) {
		int scaledMaximumWidth = maximumWidth != Integer.MAX_VALUE ? (int)(maximumWidth / getHorizontalMarginFactor()) : maximumWidth;
		final double zoomedHorizontalInsetBackup = zoomedHorizontalInset;
		final double zoomedVerticalInsetBackup = zoomedVerticalInset;
		double minimumHorizontalInset = getMinimumHorizontalInset();
		double minimumVerticalInset = getMinimumVerticalInset();
		if(mainView.isPreferredSizeSet()) {
		    final Dimension prefSize = mainView.getPreferredSize();
            prefSize.width -= 2 * Math.floor(zoomedHorizontalInset - minimumHorizontalInset);
            prefSize.height -= 2 * Math.floor(zoomedVerticalInset - minimumVerticalInset);
		    return prefSize;
		}
		else {
		    zoomedHorizontalInset = minimumHorizontalInset;
		    zoomedVerticalInset =  minimumVerticalInset;
		    final int oldMinimumWidth = mainView.getMinimumWidth();
		    final int oldMaximumWidth = mainView.getMaximumWidth();
		    final Dimension prefSize;
		    try{
		        mainView.setMinimumWidth(0);
		        mainView.setMaximumWidth(scaledMaximumWidth);
		        prefSize = super.getPreferredSize();
		        prefSize.width -= 2 * Math.floor(zoomedHorizontalInset);
		        prefSize.height -= 2 * Math.floor(zoomedVerticalInset);
		    }
		    finally {
		        zoomedHorizontalInset = zoomedHorizontalInsetBackup;
		        zoomedVerticalInset = zoomedVerticalInsetBackup;
		        mainView.setMaximumWidth(oldMaximumWidth);
		        mainView.setMinimumWidth(oldMinimumWidth);
		    }
		    return prefSize;
		}
	}

	@Override
	Insets getZoomedInsets() {
		int topInset = (int)zoomedVerticalInset;
		int leftInset = (int)zoomedHorizontalInset;
		return new Insets(topInset, leftInset, topInset, leftInset);
	}

	@Override
	void setBounds(int x, int y, int width, int height) {
		final int oldMinimumWidth = mainView.getMinimumWidth();
		mainView.setMinimumWidth(0);
		Dimension preferredRectangleSize = getPreferredRectangleSizeWithoutMargin(mainView.getMaximumWidth());
		final Dimension preferredSize = getPreferredSize();
		mainView.setMinimumWidth(oldMinimumWidth);
		super.setBounds(x, y, width, height);
		zoomedHorizontalInset = (Math.min(preferredSize.width, width) - preferredRectangleSize.width) / 2;
		zoomedVerticalInset = (Math.min(preferredSize.height, height) - preferredRectangleSize.height) / 2;
	}

	@Override
	Insets getInsets() {
		Insets insets = getZoomedInsets();
		float zoom = mainView.getZoom();
		if(zoom != 1f) {
			insets.left /= zoom;
			insets.right /= zoom;
			insets.top /= zoom;
			insets.bottom /= zoom;
		}
		return insets;
	}

	@Override
	Insets getInsets(Insets insets) {
		return getInsets();
	}

	int[] toInt(double[] relation, int offset, final int size) {
		final int[] y = new int[relation.length];
		for(int i = 0; i < relation.length; i++) {
			double relation1 = relation[i];
			y[i] = offset + (int)(size * relation1);
		}
		return y;
	}

	Polygon polygonOf(double[] xCoords, double[] yCoords, PaintOperation operation) {
		int edgeWidthOffset = mainView.getDash() != Dash.SOLID ?  mainView.getPaintedBorderWidth() * operation.borderWidthFactor : mainView.getPaintedBorderWidth();
		final Polygon polygon = new Polygon(toInt(xCoords, edgeWidthOffset/2, mainView.getWidth() - edgeWidthOffset),
			toInt(yCoords, edgeWidthOffset/2, mainView.getHeight() - edgeWidthOffset), xCoords.length);
		return polygon;
	}
}

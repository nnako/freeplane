package org.freeplane.view.swing.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.nodestyle.NodeGeometryModel;

abstract class ShapedPainter extends MainViewPainter {

	final private NodeGeometryModel shapeConfiguration;

	ShapedPainter(MainView mainView, NodeGeometryModel shapeConfiguration) {
		super(mainView);
		this.shapeConfiguration = shapeConfiguration;
	}

	@Override
	NodeGeometryModel getShapeConfiguration(){
		return shapeConfiguration;
	}

	@Override
    public Point getLeftPoint() {
		final Point in = new Point(0, mainView.getHeight() / 2);
		return in;
	}

	@Override
    public Point getRightPoint() {
		final Point in = getLeftPoint();
		in.x = mainView.getWidth() - 1;
		return in;
	}

	@Override
	void paintComponent(final Graphics graphics) {
		final Graphics2D g = (Graphics2D) graphics;
		final NodeView nodeView = mainView.getNodeView();
		if (nodeView.getNode() == null) {
			return;
		}
		mainView.paintBackgound(g);
		mainView.paintDragOver(g);
		if(mainView.getBorderColor().getAlpha() != 0) {
			paintNodeShapeConfiguringGraphics(g);
		}
		super.paintComponent(g);
	}

	private void paintNodeShapeConfiguringGraphics(final Graphics2D g) {
		final Color borderColor = mainView.getBorderColor();
		final Color oldColor = g.getColor();
		g.setColor(borderColor);
		final Stroke oldStroke = g.getStroke();
		g.setStroke(UITools.createStroke(mainView.getPaintedBorderWidth(), mainView.getDash().pattern, BasicStroke.JOIN_MITER));
		paintNodeShape(g);
		g.setColor(oldColor);
		g.setStroke(oldStroke);
	}

	abstract void paintNodeShape(final Graphics2D g);

}

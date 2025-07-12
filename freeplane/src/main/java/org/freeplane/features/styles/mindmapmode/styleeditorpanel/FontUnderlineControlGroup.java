/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2016 jberry
 *
 *  This file author is jberry
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
package org.freeplane.features.styles.mindmapmode.styleeditorpanel;

import java.beans.PropertyChangeEvent;

import org.freeplane.core.resources.components.BooleanProperty;
import org.freeplane.core.resources.components.IPropertyControl;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.NodeStyleModel;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;

import com.jgoodies.forms.builder.DefaultFormBuilder;


public class FontUnderlineControlGroup implements ControlGroup {
	static final String NODE_FONT_UNDERLINE = "nodefontunderline";

	private RevertingProperty mSetNodeFontUnderline;
	private BooleanProperty mNodeFontUnderline;
	private FontUnderlineChangeListener propertyChangeListener;

	private class FontUnderlineChangeListener extends ControlGroupChangeListener {
		public FontUnderlineChangeListener(final RevertingProperty mSet, final IPropertyControl mProperty) {
			super(mSet, mProperty);
		}

		@Override
		void applyValue(final boolean enabled, final NodeModel node, final PropertyChangeEvent evt) {
			final MNodeStyleController styleController = (MNodeStyleController) Controller
			.getCurrentModeController().getExtension(
					NodeStyleController.class);
			styleController.setUnderlined(node, enabled ? mNodeFontUnderline.getBooleanValue() : null);
		}

		@Override
		void setStyleOnExternalChange(NodeModel node) {
			final NodeStyleController styleController = NodeStyleController.getController();
			final Boolean underlined = NodeStyleModel.isUnderlined(node);
			final Boolean viewUnderlined = styleController.isUnderlined(node, StyleOption.FOR_UNSELECTED_NODE);
			mSetNodeFontUnderline.setValue(underlined != null);
			mNodeFontUnderline.setValue(viewUnderlined);
		}

        @Override
        void adjustForStyle(NodeModel node) {
            StylePropertyAdjuster.adjustPropertyControl(node, mSetNodeFontUnderline);
            StylePropertyAdjuster.adjustPropertyControl(node, mNodeFontUnderline);
        }
	}

	@Override
	public void setStyle(NodeModel node, boolean canEdit) {
		propertyChangeListener.setStyle(node);
	}

	@Override
	public void addControlGroup(DefaultFormBuilder formBuilder) {
		mSetNodeFontUnderline = new RevertingProperty();
		mNodeFontUnderline = new BooleanProperty(NODE_FONT_UNDERLINE);
		propertyChangeListener = new FontUnderlineChangeListener(mSetNodeFontUnderline, mNodeFontUnderline);
		mSetNodeFontUnderline.addPropertyChangeListener(propertyChangeListener);
		mNodeFontUnderline.addPropertyChangeListener(propertyChangeListener);
		mNodeFontUnderline.appendToForm(formBuilder);
		mSetNodeFontUnderline.appendToForm(formBuilder);
	}
}

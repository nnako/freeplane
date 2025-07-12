/**
 *
 */
package org.freeplane.plugin.script.proxy;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.NodeStyleModel;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.plugin.script.ScriptContext;

class FontProxy extends AbstractProxy<NodeModel> implements Proxy.Font {
	FontProxy(final NodeModel delegate, final ScriptContext scriptContext) {
		super(delegate, scriptContext);
	}

	public String getName() {
		return getStyleController().getFontFamilyName(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	public int getSize() {
		return getStyleController().getFontSize(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	private MNodeStyleController getStyleController() {
		return (MNodeStyleController) NodeStyleController.getController();
	}

	public boolean isBold() {
		return getStyleController().isBold(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	public boolean isBoldSet() {
		return NodeStyleModel.isBold(getDelegate()) != null;
	}

	public boolean isItalic() {
		return getStyleController().isItalic(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	public boolean isItalicSet() {
		return NodeStyleModel.isItalic(getDelegate()) != null;
	}

	@Override
	public boolean isStrikedThrough() {
		return getStyleController().isStrikedThrough(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	@Override
	public boolean isStrikedThroughSet() {
		return NodeStyleModel.isStrikedThrough(getDelegate()) != null;
	}

	@Override
	public boolean isUnderline() {
		return getStyleController().isUnderlined(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	@Override
	public boolean isUnderlineSet() {
		return NodeStyleModel.isUnderlined(getDelegate()) != null;
	}

	public boolean isNameSet() {
		return NodeStyleModel.getFontFamilyName(getDelegate()) != null;
	}

	public boolean isSizeSet() {
		return NodeStyleModel.getFontSize(getDelegate()) != null;
	}

	public void resetBold() {
		getStyleController().setBold(getDelegate(), null);
	}

	public void resetItalic() {
		getStyleController().setItalic(getDelegate(), null);
	}

	@Override
	public void resetStrikedThrough() {
		getStyleController().setStrikedThrough(getDelegate(), null);
	}

	public void resetName() {
		getStyleController().setFontFamily(getDelegate(), null);
	}

	public void resetSize() {
		getStyleController().setFontSize(getDelegate(), null);
	}

	public void setBold(final boolean bold) {
		getStyleController().setBold(getDelegate(), bold);
	}

	public void setItalic(final boolean italic) {
		getStyleController().setItalic(getDelegate(), italic);
	}

	@Override
	public void setStrikedThrough(boolean strikedThrough) {
		getStyleController().setStrikedThrough(getDelegate(), strikedThrough);
	}

	@Override
	public void setUnderline(boolean underline) {
		getStyleController().setUnderlined(getDelegate(), underline);
	}

	public void setName(final String name) {
		getStyleController().setFontFamily(getDelegate(), name);
	}

	public void setSize(final int size) {
		getStyleController().setFontSize(getDelegate(), size);
	}
}

package org.freeplane.core.ui.flatlaf;

public class FlatAtomOneDarkContrastIJTheme extends CustomThemeLaf
{
	private static final long serialVersionUID = 1L;
	public static final String NAME = "Atom One Dark Contrast (Material)";

	public FlatAtomOneDarkContrastIJTheme() {
		super( Utils.loadTheme("Atom One Dark Contrast.theme.json") );
	}

	@Override
	public String getName() {
		return NAME;
	}
}


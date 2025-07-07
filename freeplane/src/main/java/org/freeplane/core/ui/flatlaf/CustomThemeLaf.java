/*
 * Created on 7 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.core.ui.flatlaf;

import java.util.ArrayList;

import com.formdev.flatlaf.IntelliJTheme;

@SuppressWarnings("serial")
public class CustomThemeLaf extends IntelliJTheme.ThemeLaf{

	public CustomThemeLaf(IntelliJTheme theme) {
		super(theme);
	}

	@Override
	protected ArrayList<Class<?>> getLafClassesForDefaultsLoading() {
		final ArrayList<Class<?>> lafClassesForDefaultsLoading = super.getLafClassesForDefaultsLoading();
		lafClassesForDefaultsLoading.remove(IntelliJTheme.ThemeLaf.class);
		return lafClassesForDefaultsLoading;
	}

}

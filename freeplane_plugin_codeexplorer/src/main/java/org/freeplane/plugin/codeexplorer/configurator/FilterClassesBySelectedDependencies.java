/*
 * Created on 25 Nov 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.configurator;

import org.freeplane.plugin.codeexplorer.map.FilterClassesByDependencies;

@SuppressWarnings("serial")
public class FilterClassesBySelectedDependencies extends FilterClassesByDependencies {

    public FilterClassesBySelectedDependencies() {
        super("code.FilterClassesBySelectedDependencies", null);
    }
}

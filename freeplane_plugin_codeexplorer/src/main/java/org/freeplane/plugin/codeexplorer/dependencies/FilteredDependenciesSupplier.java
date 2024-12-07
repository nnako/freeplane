/*
 * Created on 24 Nov 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.dependencies;

import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.Dependency;

public interface FilteredDependenciesSupplier extends Supplier<Set<Dependency>> {/**/}
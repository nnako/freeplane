/*
 * Created on 24 Nov 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.connectors;

import java.util.Collection;
import java.util.Collections;

import org.freeplane.plugin.codeexplorer.map.CodeMap;
import org.freeplane.plugin.codeexplorer.map.CodeNode;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

public class CodeConnectorFilter {
    private Collection<Dependency> filteredDependencies;

    public CodeConnectorFilter() {
        super();
        this.filteredDependencies = Collections.emptySet();
    }

    public void setFilteredDependencies(Collection<Dependency> filteredDependencies) {
        this.filteredDependencies = filteredDependencies;
    }

    boolean isActive() {
        return ! filteredDependencies.isEmpty();
    }

    boolean isFiltered(CodeConnectorModel connector) {
        if(filteredDependencies.isEmpty())
            return true;
        return filteredDependencies.stream()
                    .anyMatch(d -> matches(d, connector));
    }

    private boolean matches(Dependency dependency, CodeConnectorModel connector) {
        CodeNode connectorSourceNode = connector.getSource();
        CodeMap map = connectorSourceNode.getMap();
        JavaClass originClass = dependency.getOriginClass();
        CodeNode originNode = map.getNodeByClass(originClass);
        if (originNode != connectorSourceNode && ! originNode.isDescendantOf(connectorSourceNode))
            return false;
        JavaClass targetClass = dependency.getTargetClass();
        CodeNode targetNode = map.getNodeByClass(targetClass);
        CodeNode connectorTargetNode = connector.getTarget();
        if (targetNode != connectorTargetNode && ! targetNode.isDescendantOf(connectorTargetNode))
            return false;
        return true;
    }
}

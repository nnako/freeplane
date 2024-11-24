/*
 * Created on 9 Nov 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.connectors;

import java.util.Objects;

import org.freeplane.features.filter.Filter;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.codeexplorer.dependencies.DependencyVerdict;
import org.freeplane.plugin.codeexplorer.map.CodeNode;

class CodeConnectorModel extends ConnectorModel {

    private final int weight;
    private final boolean goesUp;
    private final DependencyVerdict dependencyVerdict;
    CodeConnectorModel(CodeNode source, String targetID, int weight, DependencyVerdict dependencyVerdict, boolean goesUp) {
        super(source, targetID);
        this.weight = weight;
        this.goesUp = goesUp;
        this.dependencyVerdict = dependencyVerdict;
    }



    @Override
    public CodeNode getSource() {
         return (CodeNode)super.getSource();
    }

    @Override
    public CodeNode getTarget() {
         return (CodeNode)super.getTarget();
    }


    @Override
    public boolean isVisible(Filter filter) {
        return true;
    }

    int weight() {
        return weight;
    }

    boolean goesUp() {
        return goesUp;
    }

    public DependencyVerdict dependencyVerdict() {
        return dependencyVerdict;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(dependencyVerdict);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        CodeConnectorModel other = (CodeConnectorModel) obj;
        return dependencyVerdict == other.dependencyVerdict;
    }

    @Override
    public String toString() {
        return "CodeConnectorModel ["
                +  super.toString()
                + ", weight=" + weight + ", goesUp=" + goesUp
                + ", dependencyVerdict=" + dependencyVerdict + "]";
    }


}

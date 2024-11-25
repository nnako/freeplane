package org.freeplane.plugin.codeexplorer.map;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.codeexplorer.graph.GraphNodeSort;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.properties.HasName;


class ClassesNode extends CodeNode {
    static final String NODE_ID_SUFFIX = ".[package]";
    static {
        IconStoreFactory.INSTANCE.createStateIcon(ClassesNode.UI_CHILD_PACKAGE_ICON_NAME, "code/childPackage.svg");
        IconStoreFactory.INSTANCE.createStateIcon(ClassesNode.UI_SAME_PACKAGE_ICON_NAME, "code/samePackage.svg");
    }	final private JavaPackage javaPackage;
    static final String UI_CHILD_PACKAGE_ICON_NAME = "code_classes";
    static final String UI_SAME_PACKAGE_ICON_NAME = "code_same_package_classes";
    private final boolean samePackage;

	public ClassesNode(final JavaPackage javaPackage, final CodeMap map, String name, boolean samePackage, int groupIndex) {
		super(map, groupIndex);
		this.javaPackage = javaPackage;
        this.samePackage = samePackage;
		setIdWithIndex(javaPackage.getName() + NODE_ID_SUFFIX);
        long classCount = getClasses()
                .filter(jc -> isNamed(jc))
                .count();
        setText(name + formatClassCount(classCount));
        initializeChildNodes();
	}

    @Override
    Set<? extends JavaAnnotation<? extends HasName>> getAnnotations() {
        return javaPackage.getAnnotations();
    }

    @Override
    protected Stream<JavaClass> getClasses() {
        return javaPackage.getClasses().stream()
                .filter(this::belongsToSameGroup);
    }

    @Override
    HasName getCodeElement() {
        return javaPackage;
    }

    private void initializeChildNodes() {
        List<NodeModel> children = super.getChildrenInternal();
        final List<JavaClass> classes = getClasses()
                .collect(Collectors.toList());
        if(! classes.isEmpty()) {
            GraphNodeSort<JavaClass> nodeSort = new GraphNodeSort<JavaClass>();
            for (JavaClass javaClass : classes) {
                JavaClass edgeStart = findEnclosingNamedClass(javaClass);
                nodeSort.addNode(edgeStart);
                DistinctTargetDependencyFilter filter = new DistinctTargetDependencyFilter();
                Map<JavaClass, Long> dependencies = javaClass.getDirectDependenciesFromSelf().stream()
                        .filter(dep -> goesOutsideEnclosingOriginClass(edgeStart, dep))
                        .map(filter::knownDependency)
                        .filter(CodeNode::classesBelongToTheSamePackage)
                        .filter(dep -> belongsToSameGroup(dep.getTargetClass()))
                        .collect(Collectors.groupingBy(CodeNode::getTargetNodeClass, Collectors.counting()));
                dependencies.entrySet().stream()
                .forEach(e -> nodeSort.addEdge(edgeStart, e.getKey(), e.getValue()));
            }
            Map<JavaClass, ClassNode> nodes = new HashMap<>();
            List<List<JavaClass>> orderedClasses = nodeSort.sortNodes(
                    Comparator.comparing(HasName::getName),
                    SubgroupComparator.comparingByName(HasName::getName));
            for(int subgroupIndex = 0; subgroupIndex < orderedClasses.size(); subgroupIndex++) {
                for (JavaClass childClass : orderedClasses.get(subgroupIndex)) {
                    final ClassNode node = new ClassNode(childClass, getMap(), groupIndex);
                    nodes.put(childClass, node);
                    children.add(node);
                    node.setParent(this);
                }
            }
            for (JavaClass javaClass : classes) {
                JavaClass enclosingClass = findEnclosingNamedClass(javaClass);
                ClassNode node = nodes.get(enclosingClass);
                node.registerInnerClass(javaClass);
            }

        }
    }

    private boolean goesOutsideEnclosingOriginClass(JavaClass edgeStart, Dependency dependency) {
        return hasValidTopLevelClasses(dependency)
                && ! getTargetNodeClass(dependency).equals(edgeStart);
    }

    @Override
	public String toString() {
		return getText();
	}

    @Override
    Stream<Dependency> getOutgoingDependencies() {
        return getClasses()
                .flatMap(c -> c.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> hasValidTopLevelClass(dependency.getTargetClass()))
                .filter(dep -> ! dep.getTargetClass().getPackage().equals(dep.getOriginClass().getPackage())
                        || groupIndexOf(dep.getTargetClass()) != groupIndex);
    }

    @Override
    Stream<Dependency> getIncomingDependencies() {
        return getClasses()
                .flatMap(c -> c.getDirectDependenciesToSelf().stream())
                .filter(dependency -> hasValidTopLevelClass(dependency.getOriginClass()))
                .filter(dep -> ! dep.getTargetClass().getPackage().equals(dep.getOriginClass().getPackage())
                        || groupIndexOf(dep.getOriginClass()) != groupIndex);
    }


    @Override
    String getUIIconName() {
        return samePackage
                ? UI_SAME_PACKAGE_ICON_NAME
                        : UI_CHILD_PACKAGE_ICON_NAME;
    }

    @Override
    long getClassCount() {
        return getClasses().count();
    }
}

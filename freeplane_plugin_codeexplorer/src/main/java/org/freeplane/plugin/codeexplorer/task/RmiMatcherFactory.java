/*
 * Created on 6 Mar 2025
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import static com.tngtech.archunit.thirdparty.com.google.common.collect.Sets.union;

import java.rmi.Remote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.freeplane.plugin.codeexplorer.map.CodeNode;
import org.freeplane.plugin.codeexplorer.task.RmiMatcher.Mode;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.properties.HasName;

class RmiMatcherFactory {
    private final GroupMatcher matcher;
    private final JavaClasses javaClasses;
    private final Graph<GroupIdentifier, DefaultEdge> componentGraph;
    private final Map<JavaClass, GroupIdentifier> rmiClasses;
    private final RmiMatcher rmiMatcher;
    private final ClassMatcher ignoredRmi;
    private final Mode mode;

    RmiMatcherFactory(GroupMatcher matcher, JavaClasses javaClasses, Mode mode, ClassMatcher ignoredRmi){
        this.matcher = matcher;
        this.javaClasses = javaClasses;
        this.mode = mode;
        this.ignoredRmi = ignoredRmi;
        this.componentGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
        this.rmiClasses = new HashMap<>();
        fillComponentGraph();
        RmiMatcher rmiMatcher = createMatcherFromGraph();
        this.rmiMatcher = rmiMatcher;

    }
    private void fillComponentGraph() {
        javaClasses.forEach(this::addComponentsToGraph);
    }

    private RmiMatcher createMatcherFromGraph() {
        Map<String, GroupIdentifier> bundledGroups = new HashMap<>();
        ConnectivityInspector<GroupIdentifier, DefaultEdge> inspector = new ConnectivityInspector<>(componentGraph);
        List<Set<GroupIdentifier>> componentGroups = inspector.connectedSets();

        for (Set<GroupIdentifier> group : componentGroups) {
            if(group.size() > 1) {
                Set<String> finalComponentNames = new TreeSet<>();
                String id = null;
                for (GroupIdentifier componentIdentifier : group) {
                    if (componentGraph.outgoingEdgesOf(componentIdentifier).isEmpty()) {
                        finalComponentNames.add(componentIdentifier.getName());
                        if(id == null)
                            id = componentIdentifier.getId();
                    }
                }
                String name = finalComponentNames.stream().collect(Collectors.joining(", "));
                GroupIdentifier groupIdentifier = new GroupIdentifier(id, "RMI server: " + name);
                for (GroupIdentifier vertex : group) {
                    bundledGroups.put(vertex.getId(), groupIdentifier);
                }
            }
        }
        RmiMatcher rmiMatcher = new RmiMatcher(this.matcher, bundledGroups, rmiClasses);
        return rmiMatcher;
    }

    GroupMatcher createMatcher() {
        return rmiMatcher;
    }

    private void addComponentsToGraph(JavaClass javaClass) {
        if(isRemoteInterface(javaClass))
            addSubclassDependencies(Optional.empty(), null, javaClass);
    }

    private void addSubclassDependencies(Optional<GroupIdentifier> dependingGroupIdentifier, JavaClass superClass, JavaClass javaClass){
        JavaClass enclosingNamedClass = CodeNode.findEnclosingNamedClass(javaClass);
        if(! ignoredRmi.matches(enclosingNamedClass)) {
            Optional<GroupIdentifier> groupIdentifier = matcher.projectIdentifier(javaClass);
            groupIdentifier.ifPresent(gi -> {
                if (dependingGroupIdentifier.isPresent()) {
                    GroupIdentifier dgi = dependingGroupIdentifier.get();
                    if (!dgi.equals(gi)) {
                        addRmiClass(superClass, dgi);
                        addRmiClass(javaClass, gi);
                        addEdge(dgi, gi);
                    }
                }
                if(mode == Mode.INSTANTIATIONS)
                    union(javaClass.getConstructorCallsToSelf(), javaClass.getConstructorReferencesToSelf())
                    .forEach(access -> addConstructorDependencies(gi, access));
                Set<JavaClass> subclasses = javaClass.getSubclasses();
                subclasses.forEach(x -> addSubclassDependencies(groupIdentifier, javaClass, x));
            });
        }
    }
    private void addRmiClass(JavaClass javaClass, GroupIdentifier identifier) {
        rmiClasses.put(CodeNode.findEnclosingNamedClass(javaClass), identifier);
    }

    private void addConstructorDependencies(
            GroupIdentifier groupIdentifier, JavaCodeUnitAccess<? extends CodeUnitAccessTarget> access) {
        JavaClass callingClass = access.getOriginOwner();
        Optional<GroupIdentifier> callingGroupIdentifier = matcher.projectIdentifier(callingClass);
        callingGroupIdentifier.ifPresent(cgi -> {
            if (!cgi.equals(groupIdentifier)) {
                addRmiClass(callingClass, cgi);
                addRmiClass(access.getTargetOwner(), groupIdentifier);
                addEdge(groupIdentifier, cgi);
            }
        });
    }

    private void addEdge(GroupIdentifier dependingGroupIdentifier, GroupIdentifier groupIdentifier) {
        componentGraph.addVertex(dependingGroupIdentifier);
        componentGraph.addVertex(groupIdentifier);
        componentGraph.addEdge(dependingGroupIdentifier, groupIdentifier);
    }

    private static boolean isRemoteInterface(JavaClass javaClass) {
        return javaClass.isInterface() &&
                javaClass.getRawInterfaces().stream().map(HasName::getName).anyMatch(Remote.class.getName()::equals);
    }
}
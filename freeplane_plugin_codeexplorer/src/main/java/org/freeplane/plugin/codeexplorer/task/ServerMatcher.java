/*
 * Created on 20 Nov 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.rmi.Remote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.thirdparty.com.google.common.collect.Sets.union;

public class ServerMatcher implements GroupMatcher {
    private final GroupMatcher matcher;
    private Map<String, GroupIdentifier> bundledGroups;

    public static class Factory {
        private final GroupMatcher matcher;
        private final JavaClasses javaClasses;
        private final Graph<GroupIdentifier, DefaultEdge> componentGraph;
        private final ServerMatcher serverMatcher;

        public Factory(GroupMatcher matcher, JavaClasses javaClasses){
            this.matcher = matcher;
            this.javaClasses = javaClasses;
            this.componentGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
            fillComponentGraph();
            ServerMatcher serverMatcher = createMatcherFromGraph();
            this.serverMatcher = serverMatcher;

        }
        private void fillComponentGraph() {
            javaClasses.forEach(this::addComponentsToGraph);
        }

        private ServerMatcher createMatcherFromGraph() {
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
                    GroupIdentifier groupIdentifier = new GroupIdentifier(id, name);
                    for (GroupIdentifier vertex : group) {
                        bundledGroups.put(vertex.getId(), groupIdentifier);
                    }
                }
            }
            ServerMatcher serverMatcher = new ServerMatcher(this.matcher, bundledGroups);
            return serverMatcher;
        }
        public GroupMatcher createMatcher() {
            return serverMatcher;
        }

        private void addComponentsToGraph(JavaClass javaClass) {
            if(isRemoteInterface(javaClass))
                addSubclassDependencies(Optional.empty(), javaClass);
        }

        private void addSubclassDependencies(Optional<GroupIdentifier> dependingGroupIdentifier, JavaClass javaClass){
            Optional<GroupIdentifier> groupIdentifier = matcher.groupIdentifier(javaClass);
            groupIdentifier.ifPresent(gi -> {
//                union(javaClass.getConstructorCallsToSelf(), javaClass.getConstructorReferencesToSelf())
//                    .forEach(access -> addConstructorDependencies(gi, access));
                Set<JavaClass> subclasses = javaClass.getSubclasses();
                if(dependingGroupIdentifier.isPresent())
                    addEdge(dependingGroupIdentifier.get(), gi);
                subclasses.forEach(x -> addSubclassDependencies(groupIdentifier, x));
            });
        }

        private void addConstructorDependencies(
                GroupIdentifier groupIdentifier, JavaCodeUnitAccess<? extends CodeUnitAccessTarget> access) {
            JavaClass callingClass = access.getOriginOwner();
            Optional<GroupIdentifier> callingGroupIdentifier = matcher.groupIdentifier(callingClass);
            callingGroupIdentifier.ifPresent(cgi -> addEdge(groupIdentifier, cgi));
        }

        private void addEdge(GroupIdentifier dependingGroupIdentifier, GroupIdentifier groupIdentifier) {
            if(! dependingGroupIdentifier.equals(groupIdentifier)) {
                componentGraph.addVertex(dependingGroupIdentifier);
                componentGraph.addVertex(groupIdentifier);
                componentGraph.addEdge(dependingGroupIdentifier, groupIdentifier);
            }
        }

        private static boolean isRemoteInterface(JavaClass javaClass) {
            return javaClass.isInterface() &&
                    javaClass.getRawInterfaces().stream().map(HasName::getName).anyMatch(Remote.class.getName()::equals);
        }
    }

    public ServerMatcher(GroupMatcher matcher, Map<String, GroupIdentifier> bundledGroups) {
        this.matcher = matcher;
        this.bundledGroups = bundledGroups;
    }

    @Override
    public Optional<GroupIdentifier> groupIdentifier(JavaClass javaClass) {
        return matcher.groupIdentifier(javaClass).map(gi -> bundledGroups.getOrDefault(gi.getId(), gi));
    }

    @Override
    public boolean belongsToGroup(JavaClass javaClass) {
        return matcher.belongsToGroup(javaClass);
    }
}

/*
 * Created on 1 Dec 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.map;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.codeexplorer.graph.GraphNodeSort;
import org.freeplane.plugin.codeexplorer.task.CodeExplorerConfiguration;
import org.freeplane.plugin.codeexplorer.task.GroupIdentifier;
import org.freeplane.plugin.codeexplorer.task.GroupMatcher;
import org.freeplane.plugin.codeexplorer.task.GroupMatcher.MatchingCriteria;
import org.freeplane.plugin.codeexplorer.task.UserDefinedCodeExplorerConfiguration;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.properties.HasName;

class ProjectNode extends CodeNode implements GroupFinder{
    private static final int ROOT_INDEX = -1;
    static final String UI_ICON_NAME = "code_project";
    static {
        IconStoreFactory.INSTANCE.createStateIcon(ProjectNode.UI_ICON_NAME, "code/homeFolder.svg");
    }
    private static final Entry<Integer, String> UNKNOWN = new AbstractMap.SimpleEntry<>(-1, ":unknown:");
    private final JavaPackage rootPackage;
    private final Map<String, Map.Entry<Integer, String>> projectIdsByLocation;
    private final String[] idBySubrojectIndex;
    private final Set<String> badLocations;
    private final JavaClasses classes;
    private final long classCount;
    private final GroupMatcher groupMatcher;
    static ProjectNode asMapRoot(String projectName, CodeMap map, JavaClasses classes, GroupMatcher groupMatcher) {
        ProjectNode projectNode = new ProjectNode(projectName, map, classes, groupMatcher);
        map.setRoot(projectNode);
        if(projectNode.getChildCount() > 20)
            projectNode.getChildren()
                .forEach(node -> ((CodeNode)node).memoizeCodeDependencies());
        return projectNode;
    }
    private ProjectNode(String projectName, CodeMap map, JavaClasses classes, GroupMatcher groupMatcher) {
        this(projectName, map, ROOT_INDEX, classes, groupMatcher, new ConcurrentHashMap<>());
    }

    @Override
    String idWithOwnGroupIndex(String idWithoutIndex) {
        return groupIndex >= 0 ? super.idWithOwnGroupIndex(idWithoutIndex) : idWithoutIndex;
    }
    private ProjectNode(String projectName, CodeMap map, int groupIndex, JavaClasses classes, GroupMatcher groupMatcher, Map<String, Map.Entry<Integer, String>> projectIdsByLocation) {
        super(map, groupIndex);
        this.classes = classes;
        this.groupMatcher = groupMatcher;
        this.rootPackage = classes.getDefaultPackage();
        this.projectIdsByLocation = projectIdsByLocation;
        setID(idWithOwnGroupIndex(":projectRoot:"));

        if(groupIndex == ROOT_INDEX) {
            classes.stream()
            .map(groupMatcher::projectIdentifier)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(this::addLocation);
            map.setGroupFinder(this);
        }
        badLocations = new HashSet<>();
        initializeChildNodes(map);
        classCount = super.getChildrenInternal().stream()
                .map(CodeNode.class::cast)
                .mapToLong(CodeNode::getClassCount)
                .sum();
        setText(projectName + formatClassCount(classCount));
        idBySubrojectIndex = new String[projectIdsByLocation.size()];
        projectIdsByLocation.entrySet().forEach(e -> idBySubrojectIndex[e.getValue().getKey()] = e.getKey());
    }
    private void addDeletedLocation(String location) {
        final Entry<Integer, String> locationEntry = addLocation(new GroupIdentifier(location, location));
        final int childIndex = locationEntry.getKey();
        if(childIndex == getChildCount())
            insert(new DeletedContentNode(getMap(), "", childIndex, locationEntry.getValue()));
    }

    private Entry<Integer, String> addLocation(GroupIdentifier identifier) {
        return projectIdsByLocation.computeIfAbsent(identifier.getId(),
                key -> new AbstractMap.SimpleEntry<>(projectIdsByLocation.size(), identifier.getName()));
    }

    private void initializeChildNodes(CodeMap map) {
        Set<GroupIdentifier> groups =
        classes.stream()
        .map(groupMatcher::groupIdentifier)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
        List<NodeModel> children = super.getChildrenInternal();
        Map<Integer, CodeNode> nodes = groups.stream()
                .parallel()
                .map(gi ->
                    groupMatcher.subgroupMatcher(gi.getId()).map(subgroupMatcher ->
                     (CodeNode) new ProjectNode(gi.getName(), map, projectIdsByLocation.get(gi.getId()).getKey(), classes, subgroupMatcher, projectIdsByLocation))
                    .orElseGet(() ->
                    new PackageNode(rootPackage, getMap(), gi.getName(), projectIdsByLocation.get(gi.getId()).getKey(), true)))
                .collect(Collectors.toMap(x -> x.groupIndex, x -> x));
        GraphNodeSort<Integer> childNodes = new GraphNodeSort<>();
        Integer[] subrojectIndices = IntStream.range(0, projectIdsByLocation.size())
                .mapToObj(Integer::valueOf)
                .toArray(Integer[]::new);
        nodes.values()
        .stream()
        .filter(node ->node.getClassCount() > 0)
        .forEach(node -> {
            childNodes.addNode(node.groupIndex);
            DistinctTargetDependencyFilter filter = new DistinctTargetDependencyFilter();
            Map<Integer, Long> referencedGroups = node.getOutgoingDependenciesWithKnownTargets()
                    .map(filter::knownDependency)
                    .map(Dependency::getTargetClass)
                    .mapToInt(t -> groupIndexOf(t))
                    .filter(i -> i >= 0)
                    .mapToObj(i -> subrojectIndices[i])
                    .collect(Collectors.groupingBy(i -> i, Collectors.counting()));
            referencedGroups.entrySet()
            .forEach(e -> childNodes.addEdge(subrojectIndices[node.groupIndex], e.getKey(), e.getValue()));
        });
        Comparator<Set<Integer>> comparingByReversedClassCount = Comparator.comparing(
                indices -> -indices.stream()
                    .map(nodes::get)
                    .mapToLong(CodeNode::getClassCount)
                    .sum()
                );
        List<List<Integer>> orderedPackages = childNodes.sortNodes(
                Comparator.comparing(i -> nodes.get(i).getText()),
                comparingByReversedClassCount
                .thenComparing(SubgroupComparator.comparingByName(i -> nodes.get(i).getText())));
        for(int subgroupIndex = 0; subgroupIndex < orderedPackages.size(); subgroupIndex++) {
            for (Integer groupIndex : orderedPackages.get(subgroupIndex)) {
                final CodeNode node = nodes.get(groupIndex);
                children.add(node);
                node.setParent(this);
            }
        }
        if(isRoot()) {
            for(NodeModel child: children)
                ((CodeNode) child).setInitialFoldingState();
            final CodeExplorerConfiguration configuration = map.getConfiguration();
            if(configuration instanceof UserDefinedCodeExplorerConfiguration) {
                ((UserDefinedCodeExplorerConfiguration)configuration).getUserContent().keySet()
                .forEach(this::addDeletedLocation);
            }
        }

    }

    @Override
    public boolean isRoot() {
        return groupIndex == ROOT_INDEX;
    }

    @Override
    HasName getCodeElement() {
        return isRoot() ? () -> "root" : () -> "subproject";
    }

    @Override
    Stream<Dependency> getOutgoingDependencies() {
        if(isRoot())
            return Stream.empty();
        return getChildren()
            .stream()
            .map(CodeNode.class::cast)
            .flatMap(CodeNode::getOutgoingDependencies)
            .filter(d -> ! groupMatcher.belongsToGroup(d.getTargetClass()));

    }

    @Override
    Stream<Dependency> getIncomingDependencies() {
        if(isRoot())
            return Stream.empty();
        return getChildren()
                .stream()
                .map(CodeNode.class::cast)
                .flatMap(CodeNode::getIncomingDependencies)
                .filter(d -> ! groupMatcher.belongsToGroup(d.getOriginClass()));
    }

    @Override
    String getUIIconName() {
        return UI_ICON_NAME;
    }

    @Override
    public boolean isKnown(JavaClass javaClass) {
        return groupMatcher.belongsToGroup(javaClass);
    }

    @Override
    public int projectIndexOf(JavaClass javaClass) {
        return indexOf(javaClass, groupMatcher.projectIdentifier(javaClass));
    }

    private int groupIndexOf(JavaClass javaClass) {
        return indexOf(javaClass, groupMatcher.groupIdentifier(javaClass));
    }

    private int indexOf(JavaClass javaClass, Optional<GroupIdentifier> identifier) {
        Optional<String> classSourceLocation = identifier.map(GroupIdentifier::getId);
        Optional <Map.Entry<Integer, String>> groupEntry = classSourceLocation
                .map( s -> projectIdsByLocation.getOrDefault(s, UNKNOWN));

        if(groupEntry.filter(UNKNOWN::equals).isPresent() && badLocations.add(classSourceLocation.get())) {
            LogUtils.info("Unknown class source location " + javaClass.getSource().get().getUri());
         }
        return groupEntry.orElse(UNKNOWN).getKey().intValue();
    }

    @Override
    public int groupIndexOf(String location) {
        return projectIdsByLocation.getOrDefault(location, UNKNOWN).getKey().intValue();
    }

    @Override
    public Stream<JavaClass> allClasses() {
        return getClasses();
    }

    @Override
    public Stream<JavaClass> getClasses() {
        Stream<JavaClass> allClasses = classes.stream();
        return isRoot() ? allClasses : allClasses.filter(this::belongsToSameGroup);
    }

    @Override
    long getClassCount() {
         return classCount;
    }
    JavaClasses getImportedClasses() {
        return classes;
    }

    @Override
    public String getIdByIndex(int index) {
        if(index >= 0 && index < idBySubrojectIndex.length)
            return idBySubrojectIndex[index];
        else
            throw new IllegalArgumentException("Bad index " + index);
    }

    @Override
    public Optional<MatchingCriteria> matchingCriteria(JavaClass javaClass) {
        return groupMatcher.matchingCriteria(javaClass);
    }

    @Override
    public Optional<MatchingCriteria> matchingCriteria(JavaClass originClass,
            JavaClass targetClass) {
        return groupMatcher.matchingCriteria(originClass, targetClass);
    }
}

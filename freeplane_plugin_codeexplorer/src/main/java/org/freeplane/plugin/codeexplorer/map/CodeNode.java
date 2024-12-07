/*
 * Created on 9 Nov 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.map;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freeplane.core.extension.Configurable;
import org.freeplane.core.extension.IExtension;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.UIIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeRelativePath;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.codeexplorer.CodeModeController;
import org.freeplane.plugin.codeexplorer.connectors.CodeLinkController;
import org.freeplane.plugin.codeexplorer.dependencies.CodeDependency;
import org.freeplane.plugin.codeexplorer.graph.GraphCycleFinder;
import org.freeplane.plugin.codeexplorer.task.CodeAttributeMatcher;
import org.freeplane.plugin.codeexplorer.task.DependencyJudge;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.HasName;

public abstract class CodeNode extends NodeModel {

    protected final int groupIndex;

    static String formatClassCount(long classCount) {
        return " (" + classCount + (classCount == 1 ? " class)" : " classes)");
    }

    public static JavaClass findEnclosingNamedClass(JavaClass javaClass) {
        if (javaClass.isAnonymousClass())
            return findEnclosingNamedClass(javaClass.getEnclosingClass().get());
        else
            if(javaClass.isArray())
                return javaClass.getBaseComponentType();
            else
                return javaClass;
    }
    public static JavaClass findEnclosingTopLevelClass(JavaClass javaClass) {
        if (javaClass.isNestedClass())
            return findEnclosingTopLevelClass(javaClass.getEnclosingClass().get());
        else
            if(javaClass.isArray())
                return findEnclosingTopLevelClass(javaClass.getBaseComponentType());
            else
                return javaClass;
    }

    public static boolean hasValidTopLevelClass(JavaClass javaClass) {
        if(javaClass.isArray())
            return hasValidTopLevelClass(javaClass.getBaseComponentType());
        if(javaClass.isTopLevelClass())
            return -1 == javaClass.getSimpleName().indexOf('-');
        for(JavaClass enclosingClass = javaClass.getEnclosingClass().get();;
                enclosingClass = enclosingClass.getEnclosingClass().get()) {
            if(! classSourceLocationOf(enclosingClass).equals(classSourceLocationOf(javaClass)))
                return false;
            if(enclosingClass.isTopLevelClass())
                return true;
        }
    }

    public static boolean hasValidTopLevelClasses(Dependency dependency) {
        return hasValidTopLevelClass(dependency.getOriginClass()) && hasValidTopLevelClass(dependency.getTargetClass());
    }

    static String idWithGroupIndex(String idWithoutIndex, int groupIndex) {
        return idWithoutIndex + "[" + groupIndex + "]";
    }

    static JavaClass getTargetNodeClass(Dependency dependency) {
        return findEnclosingNamedClass(dependency.getTargetClass());
    }

    static boolean isNamed(JavaClass jc) {
        return ! jc.isAnonymousClass() && ! jc.isArray();
    }

    private boolean isTargetSourceKnown(Dependency dep) {
        return belongsToMap(dep.getTargetClass());
    }

    private boolean isOriginSourceKnown(Dependency dep) {
        return belongsToMap(dep.getOriginClass());
    }

    public boolean belongsToMap(JavaClass javaClass) {
        return getMap().isKnown(javaClass);
    }

    static boolean classesBelongToTheSamePackage(JavaClass first, JavaClass second) {
        return second.getPackage().equals(first.getPackage());
    }

    static boolean classesBelongToTheSamePackage(Dependency dependency) {
        return classesBelongToTheSamePackage(dependency.getOriginClass(), dependency.getTargetClass());
    }

    CodeNode(CodeMap map,  int groupIndex) {
        super(map);
        this.groupIndex = groupIndex;
    }

    void updateCodeAttributes(CodeAttributeMatcher codeAttributeMatcher) {
        NodeAttributeTableModel attributes = NodeAttributeTableModel.getModel(this);
        for(int row = attributes.getRowCount() - 1; row >= 0; row--) {
            if(attributes.getAttribute(row) instanceof CodeAttribute) {
                attributes.getAttributes().remove(row);
                attributes.fireTableRowsDeleted(this, row, row);
            }
        }

        if(! codeAttributeMatcher.isEmpty()) {
            getAnnotations().forEach(annotation -> {
                String annotationName = ClassNode.classNameWithNestedClasses(annotation.getRawType());
                annotation.getProperties().entrySet().stream()
                .filter(attributeEntry -> codeAttributeMatcher.matches(annotation, attributeEntry.getKey()))
                .forEach(attributeEntry -> addCodeAttributes(attributes, "@" +annotationName, attributeEntry.getKey(), attributeEntry.getValue()));
                if(annotation.getProperties().isEmpty()
                        && codeAttributeMatcher.matches(annotation.getRawType()))
                    addCodeAttributes(attributes, "@" + annotationName, "value", "");
            });
            getInterfaces().forEach(javaInterface -> {
                final JavaClass javaClass = javaInterface.toErasure();
                String interfaceName = ClassNode.classNameWithNestedClasses(javaClass);
                 if(codeAttributeMatcher.matches(javaClass))
                    addCodeAttributes(attributes, "interface", "value", interfaceName);
            });
         }

        getChildren().forEach(child -> ((CodeNode)child).updateCodeAttributes(codeAttributeMatcher));

    }

    private void addCodeAttributes(NodeAttributeTableModel attributes, String annotationName,
            String key, Object values) {
        Stream<?> valueStream =  values.getClass().isArray() ? Stream.of((Object[])values) : Stream.of(values);
        valueStream.forEach(value ->
            attributes.addRowNoUndo(this, new CodeAttribute(key.equals("value") ? annotationName : annotationName + "." + key, value)));
    }

    Set<? extends JavaAnnotation<? extends HasName>> getAnnotations(){
        return Collections.emptySet();
    }


    Set<JavaType> getInterfaces(){
        return Collections.emptySet();
    }

    @Override
    public CodeMap getMap() {
         return (CodeMap) super.getMap();
    }

    @Override
    public CodeNode getParentNode() {
        return (CodeNode) super.getParentNode();
    }


    String getCodeElementName() {
        return getCodeElement().getName();
    }

    void setIdWithIndex(String idWithoutIndex) {
        setID(idWithOwnGroupIndex(idWithoutIndex));
    }

    String idWithOwnGroupIndex(String idWithoutIndex) {
        return idWithGroupIndex(idWithoutIndex, groupIndex);
    }

    String idWithGroupIndex(JavaClass javaClass) {
        final JavaClass enclosingNamedClass = findEnclosingNamedClass(javaClass);
        return idWithGroupIndex(enclosingNamedClass.getName(), getMap().groupIndexOf(enclosingNamedClass));
    }

    boolean belongsToSameGroup(JavaClass javaClass) {
        return hasValidTopLevelClass(javaClass) && validClassBelongsToSameGroup(javaClass);
    }

    boolean validClassBelongsToSameGroup(JavaClass javaClass) {
        int anotherGroupIndex = projectIndexOf(javaClass);
        return anotherGroupIndex == groupIndex;
    }

    int projectIndexOf(JavaClass javaClass) {
        return getMap().groupIndexOf(javaClass);
    }

    boolean belongsToOtherGroup(JavaClass javaClass) {
        return hasValidTopLevelClass(javaClass) && ! validClassBelongsToSameGroup(javaClass);
    }

    abstract HasName getCodeElement();

    abstract long getClassCount();


    abstract Stream<Dependency> getOutgoingDependencies();
    abstract Stream<Dependency> getIncomingDependencies();
    abstract String getUIIconName();

    Stream<Dependency> getIncomingAndOutgoingDependencies(){
        return Stream.concat(getIncomingDependencies(), getOutgoingDependencies());
    }

    public Stream<Dependency> getOutgoingDependenciesWithKnownTargets(){
        return getOutgoingDependencies().filter(this::isTargetSourceKnown);
    }
    public Stream<Dependency> getIncomingDependenciesWithKnownOrigins(){
        return getIncomingDependencies().filter(this::isOriginSourceKnown);
    }
    Stream<Dependency> getIncomingAndOutgoingDependenciesWithKnownTargets(){
        return Stream.concat(getIncomingDependenciesWithKnownOrigins(), getOutgoingDependenciesWithKnownTargets());
    }

    public Stream<Dependency> getOutgoingDependenciesWithKnownTargets(Filter filter){
        final Stream<Dependency> outgoingDependenciesWithKnownTargets = getOutgoingDependenciesWithKnownTargets();
        return filter == null ? outgoingDependenciesWithKnownTargets
                : outgoingDependenciesWithKnownTargets
                .filter(dep -> filter.accepts(getMap().getNodeByClass(dep.getOriginClass())));
    }

    public Stream<Dependency> getIncomingDependenciesWithKnownOrigins(Filter filter){
        final Stream<Dependency> incomingDependenciesWithKnownOrigins = getIncomingDependenciesWithKnownOrigins();
        return filter == null ? incomingDependenciesWithKnownOrigins
                : incomingDependenciesWithKnownOrigins
                .filter(dep -> filter.accepts(getMap().getNodeByClass(dep.getTargetClass())));
    }

    Stream<Dependency> getIncomingAndOutgoingDependenciesWithKnownTargets(Filter  filter){
        return Stream.concat(getIncomingDependenciesWithKnownOrigins(filter), getOutgoingDependenciesWithKnownTargets(filter));
    }

    protected abstract Stream<JavaClass> getClasses();
    protected Stream<JavaClass> getClasses(Filter  filter){
        Stream<JavaClass> classes = getClasses();
        return filter != null ? classes.filter(javaClass -> filter.accepts(getMap().getNodeByClass(javaClass))) : classes;
    }

    Stream<JavaClass> getInheriting(){
        return getClasses()
                .flatMap(javaClass -> javaClass.getSubclasses().stream())
                .filter(this::belongsToMap);
    }
    Stream<JavaClass> getInherited(){
        return getClasses()
                .flatMap(javaClass -> Stream.concat(
                        javaClass.getRawInterfaces().stream(),
                        javaClass.getRawSuperclass().map(Stream::of).orElse(Stream.empty())))
                .filter(this::belongsToMap);
    }

    @Override
    public List<NamedIcon> getIcons() {
        UIIcon uiIcon = IconStoreFactory.ICON_STORE.getUIIcon(getUIIconName());
        return Collections.singletonList(uiIcon);
    }

    public Stream<CodeDependency> outgoingCodeDependenciesWithKnownTargets() {
        MemoizedCodeDependencies extension = getExtension(MemoizedCodeDependencies.class);
        if(extension != null)
            return extension.outgoing();
        if(getParentNode() == null || getParentNode().getChildCount() <= 40)
            return collectOutgoingCodeDependenciesWithKnownTargets();
        return memoizeCodeDependencies().outgoing();
    }

    private Stream<CodeDependency> collectOutgoingCodeDependenciesWithKnownTargets() {
        return getOutgoingDependenciesWithKnownTargets().parallel().map(getMap()::toCodeDependency);
    }

    public Stream<CodeDependency> incomingCodeDependenciesWithKnownOrigins() {
        MemoizedCodeDependencies extension = getExtension(MemoizedCodeDependencies.class);
        if(extension != null)
            return extension.incoming();
        if(getParentNode() == null || getParentNode().getChildCount() <= 40)
            return collectIncomingCodeDependenciesWithKnownOrigins();
        return memoizeCodeDependencies().incoming();
    }

    MemoizedCodeDependencies memoizeCodeDependencies() {
        MemoizedCodeDependencies extension;
        extension = new MemoizedCodeDependencies();
        addExtension(extension);
        return extension;
    }
    private Stream<CodeDependency> collectIncomingCodeDependenciesWithKnownOrigins() {
        return getIncomingDependenciesWithKnownOrigins().parallel().map(getMap()::toCodeDependency);
    }

    public static Optional<String> classSourceLocationOf(JavaClass javaClass) {
        return javaClass.getSource()
                .map(s -> {
                    URI uri = s.getUri();
                    String path = uri.getRawPath();
                    String classLocation = path != null ?  path : uri.getSchemeSpecificPart();
                    String classSourceLocation = classLocation.substring(0, classLocation.length() - javaClass.getName().length() - ".class".length());
                    return classSourceLocation;
                });
    }

    private class MemoizedCodeDependencies implements IExtension{
        DependencyJudge judge;
        CodeDependency[] incoming;
        CodeDependency[] outgoing;
        MemoizedCodeDependencies() {
            update();
        }

        private void update() {
            DependencyJudge currentJudge = getMap().getJudge();
            if (judge != currentJudge) {
                judge = currentJudge;
                incoming = collectIncomingCodeDependenciesWithKnownOrigins().toArray(CodeDependency[]::new);
                outgoing = collectOutgoingCodeDependenciesWithKnownTargets().toArray(CodeDependency[]::new);
            }
        }

        Stream<CodeDependency> incoming() {
            update();
            return Stream.of(incoming).parallel();
        }

        Stream<CodeDependency> outgoing() {
            update();
            return Stream.of(outgoing).parallel();
        }
    }

    @Override
    public <T extends IExtension> T getExtension(Class<T> clazz) {
        if (NodeLinks.class.equals(clazz)) {
            ModeController controller = Controller.getCurrentModeController();
            if(! controller.getModeName().equals(CodeModeController.MODENAME))
                return null;
            Configurable mapViewComponent = controller.getController().getMapViewManager().getMapViewConfiguration();
            if(mapViewComponent != null) {
                return (T) new CodeNodeLinks((CodeLinkController) controller.getExtension(LinkController.class), mapViewComponent, this);
            }
        }
       return super.getExtension(clazz);
    }

    void setInitialFoldingState() {
        if(getParentNode().getChildCount() > 1 && getChildCount() > 0)
            setFolded(true);
    }

    Set<CodeNode> findCyclicDependencies() {
        GraphCycleFinder<CodeNode> cycleFinder = new GraphCycleFinder<CodeNode>();
        cycleFinder.addNode(this);
        cycleFinder.stopSearchHere();
        cycleFinder.exploreGraph(Collections.singleton(this),
                this::connectedTargetNodes,
                this::connectedOriginNodes);
        Set<Entry<CodeNode, CodeNode>> cycles = cycleFinder.findSimpleCycles();

        return cycles.stream().flatMap(edge ->
        edge.getKey().getOutgoingDependenciesWithKnownTargets().flatMap(dep ->
        classNodes(edge, dep.getOriginClass(), dep.getTargetClass())))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    private Stream<CodeNode> connectedOriginNodes(CodeNode node) {
        Stream<JavaClass> originClasses = node.getIncomingDependenciesWithKnownOrigins()
        .map(Dependency::getOriginClass);
        return commonAncestorChildTowards(originClasses);
    }

    private Stream<CodeNode> connectedTargetNodes(CodeNode node) {
        Stream<JavaClass> targetClasses = node.getOutgoingDependenciesWithKnownTargets()
        .map(Dependency::getTargetClass);
        return commonAncestorChildTowards(targetClasses);
    }

    private Stream<? extends CodeNode> classNodes(Entry<CodeNode, CodeNode> edge,
            final JavaClass originClass, final JavaClass targetClass) {
        final String targetId = idWithGroupIndex(targetClass);
        final CodeNode targetClassNode = (CodeNode) getMap().getNodeForID(targetId);
        CodeNode targetNode = edge.getValue();
        if(targetNode == targetClassNode || targetClassNode.isDescendantOf(targetNode)) {
            final String originId = idWithGroupIndex(originClass);
            final CodeNode originNode = (CodeNode) getMap().getNodeForID(originId);
            return Stream.of(originNode, targetClassNode);
        }
        else
            return Stream.empty();
    }

    private Stream<CodeNode> commonAncestorChildTowards(Stream<JavaClass> classes) {
        return classes
        .map(this::idWithGroupIndex)
        .map(getMap()::getNodeForID)
        .map(CodeNode.class::cast)
        .map(node -> (CodeNode) new NodeRelativePath(this, node).endPathElement(1));
    }

}

/*
 * Created on 8 Dec 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.io.File;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.freeplane.plugin.codeexplorer.map.CodeNode;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;

public class DirectoryMatcher implements GroupMatcher{

    private static String toGroupName(String location) {
        if (location.endsWith(".jar!/")) {
            int lastSlashIndex = location.lastIndexOf('/', location.length() - ".jar!/".length());
            return location.substring(lastSlashIndex + 1, location.length() - 2);
        }

        int lastPossibleGroupNameIndex =  location.endsWith("/") ? location.length() - 1 :  location.length();

        // Search for the segments and extract the group name
        String[] segments = {"/target/", "/build/", "/bin/", "/out/"};
        for (String segment : segments) {
            int segmentIndex = location.lastIndexOf(segment);
            if (segmentIndex > 0) {
                int lastSlashIndex = location.lastIndexOf('/', segmentIndex - 1);
                return location.substring(lastSlashIndex + 1, lastPossibleGroupNameIndex);
            }
        }
        int lastSlashIndex = lastPossibleGroupNameIndex > 0 ? location.lastIndexOf('/', lastPossibleGroupNameIndex - 1) : -1;
        return location.substring(lastSlashIndex + 1, lastPossibleGroupNameIndex);
    }

    private final SortedMap<String, String> coreLocationsByPaths;
    private final Collection<File> locations;
    private final Collection<String> subpaths;
    private final Collection<ClassNameMatcher> groupMatchers;
    private final Map<String, String> groupNamesByLocation;
    private final boolean groupsClassesByName;

    public DirectoryMatcher(Collection<File> locations, Collection<String> subpaths, Collection<ClassNameMatcher> groupMatchers) {
        this.locations = locations;
        this.subpaths = subpaths;
        this.groupMatchers = groupMatchers;
        groupsClassesByName = ! groupMatchers.stream().allMatch(ClassNameMatcher::ignoresClasses);
        coreLocationsByPaths = new TreeMap<>();
        groupNamesByLocation = new HashMap<>();
        findDirectories((directory, location) -> coreLocationsByPaths.put(directory.toURI().getRawPath(), location.toURI().getRawPath()));
    }



    @Override
    public void initialize(JavaClasses javaClasses) {
        if(groupMatchers.isEmpty() || ! groupsClassesByName) {
            Set<String> updatedLocations = new HashSet<>();
            javaClasses.forEach(jc -> initializeGroupNamesForRemoteInterfaces(jc, updatedLocations));
        }
    }

    private void initializeGroupNamesForRemoteInterfaces(JavaClass javaClass, Set<String> updatedLocations) {
       if(javaClass.isInterface()
               && javaClass.isAssignableTo(Remote.class)
               && ! groupMatchers.stream().anyMatch(matcher -> matcher.isIgnored(javaClass))) {
           Optional<String> optionalPath = CodeNode.classSourceLocationOf(javaClass);
           optionalPath.ifPresent(
                   path -> {
                       if(updatedLocations.add(path)) {
                           String coreLocation = coreLocationsByPaths.getOrDefault(path, path);
                           String implementationLocation = coreLocationsByPaths.put(path, computeCoreLocationForRemoteInterface(coreLocation, javaClass));
                           javaClass.getAllSubclasses()
                           .forEach(jc ->
                           CodeNode.classSourceLocationOf(jc).ifPresent(x ->
                           {
                               if(updatedLocations.add(path))
                                   coreLocationsByPaths.put(x, implementationLocation);
                        }));
                       }
                   });
       }
    }

    private String computeCoreLocationForRemoteInterface(String coreLocation, JavaClass javaClass) {
        String implementationLocation = javaClass.getAllSubclasses().stream()
        .filter(jc -> jc.getSubclasses().isEmpty() && ! jc.isInterface() && ! jc.getModifiers().contains(JavaModifier.ABSTRACT))
        .findFirst()
        .flatMap(this::coreLocationOf)
        .orElse(coreLocation);
        return implementationLocation;
    }

    private Optional<String> coreLocationOf(JavaClass javaClass){
        return CodeNode.classSourceLocationOf(javaClass)
                .map(path -> coreLocationsByPaths.getOrDefault(path, path));
    }

    private void findDirectories(BiConsumer<File, File> consumer) {
        for(File location : locations) {
            if(location.isDirectory()) {
                for (String subPath : subpaths.isEmpty() ? defaultSubpaths(location) : subpaths) {
                    File directory = subPath.equals(".") ? location : new File(location, subPath);
                    if(directory.isDirectory())
                        consumer.accept(directory, location);
                }
            }
            else
                consumer.accept(location, location);
        }
    }

    private List<String> defaultSubpaths(File location) {
        if (new File(location, "pom.xml").exists())
            return Collections.singletonList("target/classes");
        if (new File(location, "build.gradle").exists()
                || new File(location, "build.gradle.kts").exists())
            return Arrays.asList("build/classes/java/main",
                    "build/classes/kotlin/main",
                    "build/intermediates/javac/debug/classes",
                    "build/tmp/kotlin-classes/debug");
        else
            return Collections.singletonList(".");
    }
    private Optional<String> identifierByClass(JavaClass javaClass) {
        for (ClassNameMatcher groupMatcher : groupMatchers) {
            if(groupMatcher.isIgnored(javaClass))
                return Optional.empty();
            Optional<String> groupResult = groupMatcher.toGroup(javaClass);
            if (groupResult.isPresent()) {
                return groupResult;
            }
        }
        return Optional.of("");
    }

    @Override
    public Optional<GroupIdentifier> groupIdentifier(JavaClass javaClass) {
        final Optional<String> optionalCoreLocation = coreLocationOf(javaClass);
        if(! optionalCoreLocation.isPresent())
            return Optional.empty();
        final String coreLocation = optionalCoreLocation.get();
        if(groupMatchers.isEmpty() || ! groupsClassesByName && identifierByClass(javaClass).isPresent())
            return Optional.of(new GroupIdentifier(coreLocation, groupNamesByLocation.computeIfAbsent(coreLocation, DirectoryMatcher::toGroupName)));
        else if (groupsClassesByName)
            return identifierByClass(javaClass)
                    .map(id -> id.isEmpty() ? "*" : id)
                    .map(id -> new GroupIdentifier(id, id));
        else
            return Optional.empty();
    }

    public Collection<File> getImportedLocations() {
        List<File> importedLocations = new ArrayList<>();
        findDirectories((importedLocation, location) -> importedLocations.add(importedLocation));
        return importedLocations;
    }

    public List<String> getFoundLocations(String location) {
        List<String> foundLocations = new ArrayList<>();
        for(Entry<String, String> entry : coreLocationsByPaths.tailMap(location).entrySet()) {
            if(entry.getValue().equals(location))
                foundLocations.add(entry.getKey());
            else
                break;
        }
        return foundLocations;
    }

}

/*
 * Created on 20 Nov 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

class BundlingGroupMatcher implements GroupMatcher {
    static class Factory {
    	private final GroupMatcher matcher;
    	Factory(GroupMatcher matcher, JavaClasses classes,
				Map<String, String> nameGroups) {
    		Map<String, GroupIdentifier> bundledGroups = new HashMap<>();
    		Map<String, GroupIdentifier> bundledIDentifiersByName = new HashMap<>();
    		classes.stream()
    		.map(matcher::groupIdentifier)
    		.filter(Optional::isPresent)
    		.map(Optional::get)
    		.forEach(gi -> {
        		String groupName = nameGroups.get(gi.getName());
        		if(groupName != null) {
					bundledGroups.computeIfAbsent(gi.getId(), x ->
        				bundledIDentifiersByName.computeIfAbsent(groupName,
        					y -> new GroupIdentifier(gi.getId(), groupName)));
				}
			});
    		this.matcher = new BundlingGroupMatcher(matcher, bundledGroups);
		}

		GroupMatcher createMatcher() {
			return matcher;
		}

	}

	private final GroupMatcher matcher;
    private final Map<String, GroupIdentifier> bundledGroups;
    private final Set<String> bundledGroupIds;


    BundlingGroupMatcher(GroupMatcher matcher, Map<String, GroupIdentifier> bundledGroups) {
        this.matcher = matcher;
        this.bundledGroups = bundledGroups;
        this.bundledGroupIds =bundledGroups.values().stream().map(GroupIdentifier::getId).collect(Collectors.toSet());
    }

    @Override
    public Optional<GroupIdentifier> groupIdentifier(JavaClass javaClass) {
        return matcher.groupIdentifier(javaClass)
        		.map(gi -> bundledGroups.getOrDefault(gi.getId(), gi))
        		.filter(gi -> ! gi.getName().isEmpty());
    }

    @Override
    public Optional<GroupIdentifier> projectIdentifier(JavaClass javaClass) {
        return matcher.groupIdentifier(javaClass)
        		.filter(gi -> ! bundledGroups.getOrDefault(gi.getId(), gi).getName().isEmpty());
    }

    @Override
    public boolean belongsToGroup(JavaClass javaClass) {
        return groupIdentifier(javaClass).isPresent();
    }

    @Override
    public Optional<GroupMatcher> subgroupMatcher(String id){
        if(! bundledGroupIds.contains(id))
            return Optional.empty();
        else
            return Optional.of(jc -> subgroupIdentifier(jc, id));
    }

    private Optional<GroupIdentifier> subgroupIdentifier(JavaClass javaClass, String identifier) {
        Optional<GroupIdentifier> groupIdentifier = groupIdentifier(javaClass);
        if(! groupIdentifier.isPresent() || ! groupIdentifier.get().getId().equals(identifier))
            return Optional.empty();
        else
            return matcher.groupIdentifier(javaClass);
    }

}

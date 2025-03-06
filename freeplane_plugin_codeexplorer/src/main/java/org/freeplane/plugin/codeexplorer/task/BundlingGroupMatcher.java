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
    		Map<String, String> groupedProjectIDs = classes.stream()
    				.filter(jc -> matcher.projectIdentifier(jc).isPresent())
    	            .collect(Collectors.toMap(
    	            	jc -> matcher.projectIdentifier(jc).get().getId(),
    	                jc -> matcher.groupIdentifier(jc).get().getId(),
    	                (x, y) -> x == y ? x : throwException(x, y))
    	            );
    		classes.stream()
    		.map(jc -> matcher.projectIdentifier(jc))
    		.filter(Optional::isPresent)
    		.map(Optional::get)
    		.forEach(gi -> {
        		String groupName = nameGroups.get(gi.getName());
        		if(groupName != null) {
					bundledGroups.computeIfAbsent(groupedProjectIDs.get(gi.getId()), x ->
        				bundledIDentifiersByName.computeIfAbsent(groupName,
        					y -> new GroupIdentifier(groupName, groupName)));
				}
			});
    		this.matcher = new BundlingGroupMatcher(matcher, bundledGroups);
		}

		static private String throwException(String u, String v) {
			throw new IllegalStateException(String.format("Duplicate values %s, %s", u, v));
		}

		GroupMatcher createMatcher() {
			return matcher;
		}

	}

	private final GroupMatcher matcher;
    private final Map<String, GroupIdentifier> bundledProjects;
    private final Set<String> bundledGroupIds;


    BundlingGroupMatcher(GroupMatcher matcher, Map<String, GroupIdentifier> bundledProjects) {
        this.matcher = matcher;
        this.bundledProjects = bundledProjects;
        this.bundledGroupIds =bundledProjects.values().stream().map(GroupIdentifier::getId).collect(Collectors.toSet());
    }

    @Override
    public Optional<GroupIdentifier> groupIdentifier(JavaClass javaClass) {
        return matcher.groupIdentifier(javaClass)
        		.map(gi -> bundledProjects.getOrDefault(gi.getId(), gi))
        		.filter(gi -> ! gi.getId().isEmpty());
    }

    @Override
    public Optional<GroupIdentifier> projectIdentifier(JavaClass javaClass) {
        return matcher.projectIdentifier(javaClass);
    }

    @Override
    public boolean belongsToGroup(JavaClass javaClass) {
        return groupIdentifier(javaClass).isPresent();
    }

    @Override
    public Optional<GroupMatcher> subgroupMatcher(String groupId){
        if(! bundledGroupIds.contains(groupId) && ! matcher.subgroupMatcher(groupId).isPresent())
            return Optional.empty();
        else
            return Optional.of(new GroupMatcher() {
				@Override
				public Optional<GroupIdentifier> groupIdentifier(JavaClass javaClass) {
					return subgroupIdentifier(javaClass, groupId);
				}

				@Override
				public Optional<GroupIdentifier> projectIdentifier(JavaClass javaClass) {
					 return matcher.projectIdentifier(javaClass);

				}

				@Override
				public Optional<GroupMatcher> subgroupMatcher(String id) {
					 return matcher.subgroupMatcher(id);
				}

				@Override
				public Optional<MatchingCriteria> matchingCriteria(JavaClass javaClass) {
					 return matcher.matchingCriteria(javaClass);

				}

				@Override
				public Optional<MatchingCriteria> matchingCriteria(JavaClass originClass,
						JavaClass targetClass) {
					 return matcher.matchingCriteria(originClass, targetClass);
				}
			});
    }

    private Optional<GroupIdentifier> subgroupIdentifier(JavaClass javaClass, String groupId) {
        Optional<GroupIdentifier> groupIdentifier = groupIdentifier(javaClass);
        if(! groupIdentifier.isPresent() || ! groupIdentifier.get().getId().equals(groupId))
            return Optional.empty();
        else
            return matcher.groupIdentifier(javaClass);
    }

}

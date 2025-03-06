/*
 * Created on 6 Mar 2025
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClasses;

class LocationMatcherFactory {
	private final GroupMatcher matcher;
	LocationMatcherFactory(GroupMatcher matcher, JavaClasses classes,
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
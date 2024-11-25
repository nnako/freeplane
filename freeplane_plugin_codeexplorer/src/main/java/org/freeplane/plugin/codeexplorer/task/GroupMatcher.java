/*
 * Created on 8 Feb 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.Optional;

import com.tngtech.archunit.core.domain.JavaClass;

@SuppressWarnings("unused")
public interface GroupMatcher {
    public enum MatchingCriteria{RMI}
    Optional<GroupIdentifier> groupIdentifier(JavaClass javaClass);
    default boolean belongsToGroup(JavaClass javaClass) {
        return groupIdentifier(javaClass).isPresent();
    }

    default Optional<GroupMatcher> subgroupMatcher(String id) {
        return Optional.empty();
    }

    default Optional<MatchingCriteria> matchingCriteria(JavaClass javaClass){
        return Optional.empty();
    }

    default Optional<MatchingCriteria> matchingCriteria(JavaClass originClass, JavaClass targetClass){
        return Optional.empty();
    }
}

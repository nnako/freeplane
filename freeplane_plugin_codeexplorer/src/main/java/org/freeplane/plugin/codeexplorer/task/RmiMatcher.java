/*
 * Created on 20 Nov 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.Map;
import java.util.Optional;

import org.freeplane.plugin.codeexplorer.map.CodeNode;

import com.tngtech.archunit.core.domain.JavaClass;

class RmiMatcher extends BundlingGroupMatcher implements GroupMatcher {

    enum Mode {IMPLEMENTATIONS, INSTANTIATIONS}

    private final Map<JavaClass, GroupIdentifier> rmiClasses;


    RmiMatcher(GroupMatcher matcher, Map<String, GroupIdentifier> bundledGroups, Map<JavaClass, GroupIdentifier> rmiClasses) {
    	super(matcher, bundledGroups);
        this.rmiClasses = rmiClasses;
    }

    @Override
    public Optional<MatchingCriteria> matchingCriteria(JavaClass javaClass){
        return rmiClasses.containsKey(CodeNode.findEnclosingNamedClass(javaClass)) ? Optional.of(MatchingCriteria.RMI) : Optional.empty();
    }

    @Override
    public Optional<MatchingCriteria> matchingCriteria(JavaClass originClass, JavaClass targetClass){
        GroupIdentifier originIdentifier = rmiClasses.get(CodeNode.findEnclosingNamedClass(originClass));
        if(originIdentifier == null)
            return Optional.empty();
        GroupIdentifier targetIdentifier = rmiClasses.get(CodeNode.findEnclosingNamedClass(targetClass));
        if(targetIdentifier == null)
            return Optional.empty();
        return ! originIdentifier.equals(targetIdentifier) ? Optional.of(MatchingCriteria.RMI) : Optional.empty();
    }
}

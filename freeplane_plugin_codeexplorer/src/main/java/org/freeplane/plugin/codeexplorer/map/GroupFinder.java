/*
 * Created on 3 Dec 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.map;

import java.util.Optional;
import java.util.stream.Stream;

import org.freeplane.plugin.codeexplorer.task.GroupMatcher.MatchingCriteria;

import com.tngtech.archunit.core.domain.JavaClass;

interface GroupFinder {
    GroupFinder EMPTY = new GroupFinder() {

        @Override
        public int projectIndexOf(JavaClass javaClass) {
            return -1;
        }

        @Override
        public Stream<JavaClass> allClasses() {
            return Stream.empty();
        }

        @Override
        public String getIdByIndex(int index) {
           throw new IllegalArgumentException("No locations");
        }

        @Override
        public int groupIndexOf(String groupId) {
            return -1;
        }

        @Override
        public boolean isKnown(JavaClass javaClass) {
             return false;
        }

        @Override
        public Optional<MatchingCriteria> matchingCriteria(JavaClass javaClass) {
             return Optional.empty();
        }

        @Override
        public Optional<MatchingCriteria> matchingCriteria(JavaClass originClass, JavaClass targetClass) {
             return Optional.empty();
        }
    };
    boolean isKnown(JavaClass javaClass);
    int projectIndexOf(JavaClass javaClass);
    int groupIndexOf(String groupId);
    String getIdByIndex(int index);
    Stream<JavaClass> allClasses();
    Optional<MatchingCriteria> matchingCriteria(JavaClass javaClass);
    Optional<MatchingCriteria> matchingCriteria(JavaClass originClass, JavaClass targetClass);
}

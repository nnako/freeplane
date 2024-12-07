/*
 * Created on 8 Dec 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;

class ClassMatcher implements ImportOption{
    private static final Pattern CLASS_LOCATION_PATTERN = Pattern.compile("(?<=/)[\\w/]+(?=(?:\\$[\\w\\$/]*)?\\.class$)");
    private final List<PackageMatcher> matchers;
    private final List<String> patterns;

    ClassMatcher(List<String> patterns) {
        super();
        this.patterns = patterns;
        this.matchers = patterns.stream()
                .map(s -> s.startsWith("..") ? s : ".." + s)
                .map(PackageMatcher::of).collect(Collectors.toList());
    }

    boolean matches(Location location) {
        String locationString = location.asURI().toString();
        if(locationString.endsWith("/package-info.class"))
            return true;
        Matcher matcher = CLASS_LOCATION_PATTERN.matcher(locationString);
        if (matcher.find()) {
            String namedClass = matcher.group().replace('/', '.');
            return matches(namedClass);
        } else
            return false;
    }

    boolean matches(JavaClass javaClass) {
        return matches(javaClass.getFullName());
    }

    private boolean matches(String className) {
        return matchers.stream().anyMatch(m -> m.matches(className));
    }

    @Override
    public int hashCode() {
        return Objects.hash(patterns);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassMatcher other = (ClassMatcher) obj;
        return Objects.equals(patterns, other.patterns);
    }

    @Override
    public boolean includes(Location location) {
       return matchers.isEmpty() ? true : ! matches(location);
    }
}

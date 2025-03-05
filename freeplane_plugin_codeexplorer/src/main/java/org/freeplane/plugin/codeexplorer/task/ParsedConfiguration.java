/*
 * Created on 28 Nov 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.codeexplorer.dependencies.DependencyDirection;
import org.freeplane.plugin.codeexplorer.dependencies.DependencyRule;
import org.freeplane.plugin.codeexplorer.dependencies.DependencyVerdict;
import org.freeplane.plugin.codeexplorer.task.RmiMatcher.Mode;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;

public class ParsedConfiguration {
    public static final String HELP;
    static {
        final Controller currentController = Controller.getCurrentController();
        HELP = currentController != null ? currentController.getResourceController().loadString("/org/freeplane/plugin/codeexplorer/documentation.txt") :  "";
    }

    private static final String CLASS_PATTERN = "[\\w\\.\\|\\(\\)\\*\\[\\]]+";

    private static final String LOCATION_PATTERN = "[\\w\\.-]+(?:\\s*,\\s*[\\w\\.-]+)*";

    private static final String DIRECTION_PATTERN = Pattern.quote(DependencyDirection.UP.notation)
            + "|" + Pattern.quote(DependencyDirection.DOWN.notation)
            + "|" + Pattern.quote(DependencyDirection.ANY.notation);

    private static final Pattern DEPENDENCY_RULE_PATTERN = Pattern.compile(
            "^\\s*(" + DependencyVerdict.ALLOWED.keyword + "|"
            + DependencyVerdict.FORBIDDEN.keyword + "|"
            + DependencyVerdict.IGNORED.keyword + ")\\s+"
            + "(" + CLASS_PATTERN + ")\\s*"
            + "(" + DIRECTION_PATTERN + ")"
            + "\\s*("+ CLASS_PATTERN + ")\\s*$");

    private static final Pattern CLASSPATH_PATTERN = Pattern.compile(
            "^\\s*classpath\\s+/*(.*\\S)\\s*$");

    private static final Pattern IGNORED_CLASS_PATTERN = Pattern.compile(
            "^\\s*ignore\\s+class\\s+(" + CLASS_PATTERN + ")\\s*$");

    private static final Pattern IMPORTED_ANNOTATION_PATTERN = Pattern.compile(
            "^\\s*import\\s+(annotation|interface)\\s+(" + CLASS_PATTERN + ")\\s*$");

    private static final Pattern GROUP_PATTERN = Pattern.compile(
            "^\\s*(?:(ignore)\\s+)?(?:(class)\\s+)?group\\s+(" + CLASS_PATTERN + ")(?:\\s+as\\s+(.*?))?\\s*$");

    private static final Pattern LOCATION_GROUP_PATTERN = Pattern.compile(
            "^\\s*(?:(ignore)\\s+)?location\\s+group\\s+(" + LOCATION_PATTERN + ")(?:\\s+as\\s+(.*?))?\\s*$");


    private static final Pattern GROUP_RMI = Pattern.compile(
            "^\\s*group\\s+(?:RMI|rmi)(\\s+instances)?\\s*$");

    private static final Pattern IGNORED_RMI_PATTERN = Pattern.compile(
            "^\\s*ignore\\s+(?:RMI|rmi)\\s+(" + CLASS_PATTERN + ")\\s*$");

    private final List<DependencyRule> rules;
    private final ClassMatcher ignoredClasses;
    private final CodeAttributeMatcher codeAttributeMatcher;
    private final List<String> subpaths;
    private final List<ClassNameMatcher> groupMatchers;
    private final Map<String, String> locationGroups;
    private final Optional<RmiMatcher.Mode> rmiMatcherMode;
    private final ClassMatcher ignoredRmi;



    public ParsedConfiguration(String dsl) {
        List<DependencyRule> dependencyRules = new ArrayList<>();
        List<String> ignoredClasses = new ArrayList<>();
        List<String> importedAnnotations = new ArrayList<>();
        List<String> subpaths = new ArrayList<>();
        List<ClassNameMatcher> groupMatchers = new ArrayList<>();
        RmiMatcher.Mode rmiMatcherMode = null;
        List<String> ignoredRmi = new ArrayList<>();
        Map<String, String> locationGroups = new HashMap<>();

        String[] dslRules = dsl.split("\\n\\s*");

        String dslRule = "";
        for (String dslRuleLine : dslRules) {
            dslRule = dslRule.endsWith(",") ? dslRule + dslRuleLine.trim()
            : dslRule.endsWith("\\") ? dslRule.substring(0, dslRule.length() - 1) + dslRuleLine.trim()
            : dslRuleLine.trim();
            if(dslRule.isEmpty() || dslRule.startsWith("#") || dslRule.startsWith("//") || dslRule.endsWith(",") || dslRule.endsWith("\\"))
                continue;
            Matcher dependencyMatcher = DEPENDENCY_RULE_PATTERN.matcher(dslRule);
            if (dependencyMatcher.find()) {
                DependencyVerdict type = DependencyVerdict.parseVerdict(dependencyMatcher.group(1));
                String originPattern = dependencyMatcher.group(2);
                String directionNotation = dependencyMatcher.group(3);
                String targetPattern = dependencyMatcher.group(4);

                DependencyDirection dependencyDirection = DependencyDirection.parseDirection(directionNotation);

                DependencyRule rule = new DependencyRule(type, originPattern, targetPattern, dependencyDirection);
                dependencyRules.add(rule);
                continue;
            }
            Matcher classpathMatcher = CLASSPATH_PATTERN.matcher(dslRule);
            if (classpathMatcher.find()) {
                subpaths.add(classpathMatcher.group(1));
                continue;
            }
            Matcher ignoredClassMatcher = IGNORED_CLASS_PATTERN.matcher(dslRule);
            if (ignoredClassMatcher.find()) {
                ignoredClasses.add(ignoredClassMatcher.group(1));
                continue;
            }
            Matcher groupRmiMatcher = GROUP_RMI.matcher(dslRule);
            if (groupRmiMatcher.find()) {
                rmiMatcherMode = groupRmiMatcher.group(1) != null ? Mode.INSTANTIATIONS : Mode.IMPLEMENTATIONS;
                continue;
            }
            Matcher ignoredRmiMatcher = IGNORED_RMI_PATTERN.matcher(dslRule);
            if (ignoredRmiMatcher.find()) {
                ignoredRmi.add(ignoredRmiMatcher.group(1));
                continue;
            }
            Matcher groupPatternMatcher = GROUP_PATTERN.matcher(dslRule);
            if (groupPatternMatcher.find()) {
                final boolean ignores = groupPatternMatcher.group(1) != null;
                final boolean matchesClasses = groupPatternMatcher.group(2) != null;
                final String pattern = groupPatternMatcher.group(3);
                final Optional<String> name = Optional.ofNullable(groupPatternMatcher.group(4));
                groupMatchers.add(new ClassNameMatcher(pattern, ignores, matchesClasses, name));
                continue;
            }
            Matcher locationGroupPatternMatcher = LOCATION_GROUP_PATTERN.matcher(dslRule);
            if (locationGroupPatternMatcher.find()) {
                final boolean ignores = locationGroupPatternMatcher.group(1) != null;
                final String[] locations = locationGroupPatternMatcher.group(2).split("\\s*,\\s*");
                final String name = ignores ? "" : Optional.ofNullable(locationGroupPatternMatcher.group(3)).orElse(locations[0]);
                Arrays.asList(locations).forEach(location -> locationGroups.computeIfAbsent(location, x -> name));
                continue;
            }
            Matcher importedAnnotationMatcher = IMPORTED_ANNOTATION_PATTERN.matcher(dslRule);
            if(importedAnnotationMatcher.find()) {
                final String annotationPattern = importedAnnotationMatcher.group(2);
                if(annotationPattern.endsWith("()") && importedAnnotationMatcher.group(1).equals("interface"))
                    throw new IllegalArgumentException("Invalid rule " + dslRule);
                importedAnnotations.add(annotationPattern);
                continue;
            }
            throw new IllegalArgumentException("Invalid rule " + dslRule);

        }
        this.rules = dependencyRules;
        this.ignoredClasses = new ClassMatcher(ignoredClasses);
        this.codeAttributeMatcher = new CodeAttributeMatcher(importedAnnotations);
        this.subpaths = subpaths;
        this.groupMatchers = groupMatchers;
        this.locationGroups = locationGroups;
        this.rmiMatcherMode = Optional.ofNullable(rmiMatcherMode);
        this.ignoredRmi = new ClassMatcher(ignoredRmi);
    }

    public DependencyRuleJudge judge() {
        return new DependencyRuleJudge(rules);
    }

    public CodeAttributeMatcher codeAttributeMatcher() {
        return codeAttributeMatcher;
    }

    public DirectoryMatcher createDirectoryMatcher(Collection<File> locations) {
        return new DirectoryMatcher(locations, subpaths, groupMatchers);
    }

    public ImportOption importOption() {
        return ignoredClasses;
    }

    public ConfigurationChange configurationChange(ParsedConfiguration previousConfiguration) {
        if(previousConfiguration == null
                || ! subpaths.equals(previousConfiguration.subpaths)
                || ! ignoredClasses.equals(previousConfiguration.ignoredClasses))
            return ConfigurationChange.CODE_BASE;
        if(! rules.equals(previousConfiguration.rules)
                || ! codeAttributeMatcher.equals(previousConfiguration.codeAttributeMatcher))
                return ConfigurationChange.CONFIGURATION;
        if(! groupMatchers.equals(previousConfiguration.groupMatchers)
                || ! ignoredRmi.equals(previousConfiguration.ignoredRmi)
                || ! rmiMatcherMode.equals(previousConfiguration.rmiMatcherMode)
                || ! locationGroups.equals(previousConfiguration.locationGroups))
                return ConfigurationChange.GROUPS;
        return ConfigurationChange.SAME;
    }

    public GroupMatcher createGroupMatcher(Set<File> projectLocations, JavaClasses classes) {
        DirectoryMatcher directoryMatcher = createDirectoryMatcher(projectLocations);
        GroupMatcher matcher = rmiMatcherMode.map(mode ->
                new RmiMatcher.Factory(directoryMatcher, classes, mode, ignoredRmi).createMatcher())
            .orElse(directoryMatcher);
        if(locationGroups.isEmpty())
        	return matcher;
        return new BundlingGroupMatcher.Factory(matcher, classes, locationGroups).createMatcher();
    }
}
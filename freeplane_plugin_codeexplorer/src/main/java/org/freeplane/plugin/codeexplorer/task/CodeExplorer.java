/*
 * Created on 25 Nov 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

public interface CodeExplorer {
    void explore(CodeExplorerConfiguration configuration, boolean reloadCodebase);
    void setProjectConfiguration(DependencyJudge judge, CodeAttributeMatcher codeAttributeMatcher);
    void cancelAnalysis();
}

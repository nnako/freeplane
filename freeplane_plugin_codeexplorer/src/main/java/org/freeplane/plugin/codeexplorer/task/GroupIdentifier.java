/*
 * Created on 8 Mar 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.Objects;

public class GroupIdentifier {
    private final String id;
    private final String name;
    public GroupIdentifier(String id, String name) {
        super();
        this.id = id;
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GroupIdentifier other = (GroupIdentifier) obj;
        return Objects.equals(id, other.id);
    }
}

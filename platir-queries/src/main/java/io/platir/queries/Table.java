package io.platir.queries;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
class Table<T> {

    private String name;
    private String updateTime;
    private final Set<T> rows = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Set<T> rows() {
        return rows;
    }

}

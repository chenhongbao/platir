package io.platir.queries;

import io.platir.service.User;

/**
 *
 * @author Chen Hongbao
 */
class UserImpl implements User {

    private String userId;
    private String password;
    private String createTime;
    private String lastLoginTime;

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String getLastLoginTime() {
        return lastLoginTime;
    }

    @Override
    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

}

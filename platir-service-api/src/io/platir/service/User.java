package io.platir.service;

public class User {

	private String userId;
	private String password;
	private String createTime;
	private String lastLoginTime;

	public String getUserId() {
		return userId;
	}

	public String getPassword() {
		return password;
	}

	public String getCreateTime() {
		return createTime;
	}

	public String getLastLoginTime() {
		return lastLoginTime;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public void setLastLoginTime(String lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

}

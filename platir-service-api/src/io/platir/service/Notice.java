package io.platir.service;

public class Notice {
	private String message;
	private Integer code;
	private Object object;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
	
	public boolean isGood() {
		return code != null && code == 0;
	}

}

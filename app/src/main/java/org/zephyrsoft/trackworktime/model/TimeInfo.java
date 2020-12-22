package org.zephyrsoft.trackworktime.model;

public class TimeInfo {
	private long actual = 0;
	private long target = 0;

	public Long getActual() {
		return actual;
	}

	public void setActual(long actual) {
		this.actual = actual;
	}

	public Long getTarget() {
		return target;
	}

	public void setTarget(long target) {
		this.target = target;
	}

	public Long getBalance() {
		return actual - target;
	}
}

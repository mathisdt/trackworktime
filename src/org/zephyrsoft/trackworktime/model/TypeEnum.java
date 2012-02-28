package org.zephyrsoft.trackworktime.model;

/**
 * @author Mathis Dirksen-Thedens
 */
public enum TypeEnum {
	
	CLOCK_IN(1), CLOCK_OUT(0);
	
	private Integer value = null;
	
	private TypeEnum(Integer value) {
		this.value = value;
	}
	
	public Integer getValue() {
		return value;
	}
}

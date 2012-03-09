package org.zephyrsoft.trackworktime.model;

/**
 * The possible event types - clock in or clock out.
 * 
 * @author Mathis Dirksen-Thedens
 */
public enum TypeEnum {
	
	/**
	 * clock-in type of event
	 */
	CLOCK_IN(1),
	/**
	 * clock-out type of event
	 */
	CLOCK_OUT(0);
	
	private Integer value = null;
	
	private TypeEnum(Integer value) {
		this.value = value;
	}
	
	/**
	 * Gets the value of this enum for storing it in database.
	 */
	public Integer getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
	
}

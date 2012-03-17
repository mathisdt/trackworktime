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
	CLOCK_IN(Values.CLOCK_IN_VALUE),
	/**
	 * clock-out type of event
	 */
	CLOCK_OUT(Values.CLOCK_OUT_VALUE),
	/**
	 * clock-out now type of event used to display correct amount of worked time on current day when currently clocked
	 * in - THIS TYPE NEVER COMES FROM THE DATABASE
	 */
	CLOCK_OUT_NOW(Values.CLOCK_OUT_NOW_VALUE);
	
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
	
	/**
	 * Get the enum for a specific value.
	 * 
	 * @param value the value
	 * @return the corresponding enum
	 */
	public static TypeEnum byValue(Integer value) {
		if (value == null) {
			return null;
		} else if (value == Values.CLOCK_IN_VALUE) {
			return CLOCK_IN;
		} else if (value == Values.CLOCK_OUT_VALUE) {
			return CLOCK_OUT;
		} else if (value == Values.CLOCK_OUT_NOW_VALUE) {
			return CLOCK_OUT_NOW;
		} else {
			throw new IllegalArgumentException("unknown value");
		}
	}
	
	private static class Values {
		private static final int CLOCK_IN_VALUE = 1;
		private static final int CLOCK_OUT_VALUE = 0;
		private static final int CLOCK_OUT_NOW_VALUE = -1;
	}
}

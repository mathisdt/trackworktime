package org.zephyrsoft.worktimetracker.model;

public class Base {
	
	/**
	 * Chainable compare operation.
	 */
	@SuppressWarnings("null")
	protected int compare(Object attributeOfMe, Object attributeOfOther, int useIfEqual) {
		if (attributeOfMe == null && attributeOfOther == null) {
			return useIfEqual;
		} else if (attributeOfMe != null && attributeOfOther == null) {
			return 1;
		} else if (attributeOfMe == null && attributeOfOther != null) {
			return -1;
		} else {
			return attributeOfMe.toString().compareTo(attributeOfOther.toString());
		}
	}
	
}

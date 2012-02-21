package org.zephyrsoft.trackworktime.model;

public class Week extends Base implements Comparable<Week> {
	private Integer id = null;
	private String start = null;
	private Integer sum = null;
	
	public Week() {
		// do nothing
	}
	
	public Week(Integer id, String start, Integer sum) {
		this.id = id;
		this.start = start;
		this.sum = sum;
	}
	
	public Integer getId() {
		return id;
	}
	
	public String getStart() {
		return start;
	}
	
	public Integer getSum() {
		return sum;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public void setStart(String start) {
		this.start = start;
	}
	
	public void setSum(Integer sum) {
		this.sum = sum;
	}
	
	@Override
	public int compareTo(Week another) {
		return compare(getStart(), another.getStart(), compare(getId(), another.getId(), 0));
	}
	
	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 * 
	 * @see org.zephyrsoft.trackworktime.model.Base#toString()
	 */
	@Override
	public String toString() {
		return getStart() + " - " + getSum();
	}
}

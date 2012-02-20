package org.zephyrsoft.worktimetracker.model;

public class Week extends Base implements Comparable<Week> {
	private Integer id = null;
	private String start = null;
	private Integer sum = null;
	
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
}

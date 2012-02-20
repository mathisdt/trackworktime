package org.zephyrsoft.worktimetracker.model;

public class Task extends Base implements Comparable<Task> {
	private Integer id = null;
	private String name = null;
	private Integer active = null;
	
	public Task(Integer id, String name, Integer active) {
		this.id = id;
		this.name = name;
		this.active = active;
	}
	
	public Integer getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public Integer getActive() {
		return active;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setActive(Integer active) {
		this.active = active;
	}
	
	@Override
	public int compareTo(Task another) {
		return compare(getName(), another.getName(), compare(getId(), another.getId(), 0));
	}
}

package org.zephyrsoft.trackworktime.model;

public class Task extends Base implements Comparable<Task> {
	private Integer id = null;
	private String name = null;
	private Integer active = null;
	private Integer ordering = null;
	
	public Task() {
		// do nothing
	}
	
	public Task(Integer id, String name, Integer active, Integer ordering) {
		this.id = id;
		this.name = name;
		this.active = active;
		this.ordering = ordering;
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
	
	public Integer getOrdering() {
		return ordering;
	}
	
	public void setOrdering(Integer ordering) {
		this.ordering = ordering;
	}
	
	@Override
	public int compareTo(Task another) {
		return compare(getName(), another.getName(), compare(getId(), another.getId(), 0));
	}
	
	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 * 
	 * @see org.zephyrsoft.trackworktime.model.Base#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
}

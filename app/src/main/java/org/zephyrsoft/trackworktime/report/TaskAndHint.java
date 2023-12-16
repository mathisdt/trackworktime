package org.zephyrsoft.trackworktime.report;

import org.zephyrsoft.trackworktime.model.Base;
import org.zephyrsoft.trackworktime.model.Task;

/**
 * Comparable class that holds a task and the hint/text for a event
 **/
public class TaskAndHint extends Base implements Comparable<TaskAndHint> {

    private String text;
    private Task task;

    public TaskAndHint() {
        // do nothing
    }

    public TaskAndHint(String text, Task task) {
        this.text = text;
        this.task = task;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public int compareTo(TaskAndHint another) {
        return compare(getTask(), another.getTask(), compare(getText(), another.getText(), 0));
    }

    @Override
    public String toString() {
        return task.toString() + " " + (this.text == null ? "" : "'" + this.text + "'");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((task != null) ? 0 : task.hashCode());
        result = prime * result + ((text != null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        TaskAndHint other = (TaskAndHint) obj;
        if (task == null) {
            if (other.getTask() == null) {
                if (text == null) {
                    return other.getText() == null;
                }
                return text.equals(other.getText());
            }
            return false;
        } else if (text == null) {
            if (other.getText() == null) {
                return task.equals(other.getTask());
            }
            return false;
        } else {
            return task.equals(other.getTask()) && text.equals(other.getText());
        }
    }
}

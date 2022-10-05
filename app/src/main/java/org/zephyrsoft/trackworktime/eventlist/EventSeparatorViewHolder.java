package org.zephyrsoft.trackworktime.eventlist;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.zephyrsoft.trackworktime.databinding.ListItemSeparatorBinding;

public class EventSeparatorViewHolder extends ViewHolder {
	private final ListItemSeparatorBinding binding;

	public EventSeparatorViewHolder(ListItemSeparatorBinding binding) {
		super(binding.getRoot());
		this.binding = binding;
	}

	public void bind(EventSeparatorItem item) {
		binding.title.setText(item.getTitle());
	}
}


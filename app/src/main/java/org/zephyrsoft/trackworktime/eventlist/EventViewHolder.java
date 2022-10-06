package org.zephyrsoft.trackworktime.eventlist;

import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.zephyrsoft.trackworktime.databinding.ListItemBinding;
import org.zephyrsoft.trackworktime.model.Event;

import java.util.function.Consumer;

public class EventViewHolder extends ViewHolder {
	final ListItemBinding binding;

	public EventViewHolder(ListItemBinding binding) {
		super(binding.getRoot());
		this.binding = binding;
	}

	public void bind(EventItem item, boolean isSelected, Consumer<Event> onClick) {
		binding.time.setText(item.getTime());
		binding.type.setText(item.getType());
		binding.task.setText(item.getTask());
		itemView.setActivated(isSelected);
		itemView.setOnClickListener(v -> onClick.accept(item.getEvent()));
	}

	public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
		return new ItemDetailsLookup.ItemDetails<>() {
			@Override
			public int getPosition() {
				return getBindingAdapterPosition();
			}

			@Override
			public Long getSelectionKey() {
				return getItemId();
			}
		};
	}
}

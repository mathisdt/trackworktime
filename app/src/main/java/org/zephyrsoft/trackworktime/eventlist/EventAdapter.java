package org.zephyrsoft.trackworktime.eventlist;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.zephyrsoft.trackworktime.databinding.ListItemBinding;
import org.zephyrsoft.trackworktime.databinding.ListItemSeparatorBinding;
import org.zephyrsoft.trackworktime.model.Event;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class EventAdapter extends ListAdapter<BaseEventItem, ViewHolder> {

	private static final int VIEW_TYPE_SEPARATOR = 0;
	private static final int VIEW_TYPE_EVENT = 1;

	private final Consumer<Event> onEventClick;
	private final EventItemMapper itemMapper;
	private final Predicate<Event> isEventSelected;

	private static final ItemCallback<BaseEventItem> ITEM_CALLBACK = new ItemCallback<>() {
		@Override
		public boolean areItemsTheSame(@NonNull BaseEventItem old, @NonNull BaseEventItem neu) {
			return old.isSameIdAs(neu);
		}

		@Override
		public boolean areContentsTheSame(@NonNull BaseEventItem old, @NonNull BaseEventItem neu) {
			return old.isSameContentAs(neu);
		}
	};

	public EventAdapter(
			@NonNull Consumer<Event> onEventClick,
			@NonNull Locale locale,
			@NonNull Function<Event, String> eventTaskName,
			@NonNull Predicate<Event> isEventSelected
	) {
		super(ITEM_CALLBACK);
		this.itemMapper = new EventItemMapper(locale, eventTaskName);
		this.onEventClick = onEventClick;
		this.isEventSelected = isEventSelected;
		setHasStableIds(true);
	}

	public void submitEvents(List<Event> events) {
		var items = itemMapper.map(events);
		submitList(items);
	}

	@Override
	public int getItemViewType(int position) {
		if (getItem(position) instanceof EventSeparatorItem) {
			return VIEW_TYPE_SEPARATOR;
		} else {
			return VIEW_TYPE_EVENT;
		}
	}

	@Override
	@NonNull
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		switch(viewType) {
			case VIEW_TYPE_SEPARATOR: {
				ListItemSeparatorBinding binding = ListItemSeparatorBinding.inflate(inflater,
						parent, false);
				return new EventSeparatorViewHolder(binding);
			} case VIEW_TYPE_EVENT: {
				ListItemBinding binding = ListItemBinding.inflate(inflater, parent, false);
				return new EventViewHolder(binding);
			} default: {
				throw new RuntimeException("Not implemented type: " + viewType);
			}
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		BaseEventItem item = getItem(position);
		if (holder instanceof EventViewHolder) {
			EventViewHolder eventHolder = (EventViewHolder) holder;
			EventItem eventItem = (EventItem) item;
			boolean isSelected = this.isEventSelected.test(eventItem.getEvent());
			eventHolder.bind((EventItem) item, isSelected, onEventClick);
		} else if (holder instanceof EventSeparatorViewHolder) {
			EventSeparatorViewHolder eventHolder = (EventSeparatorViewHolder) holder;
			eventHolder.bind((EventSeparatorItem) item);
		} else {
			throw new RuntimeException("Not implemented view holder type: " + holder);
		}
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}
}


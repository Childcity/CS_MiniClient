package com.cardsystems.stopnet4.monitoring;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<EventData> events;

    public EventListAdapter(Context context, List<EventData> events) {
        this.events = events;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public EventListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventListAdapter.ViewHolder holder, int position) {
        EventData event = events.get(position);
        holder.imageView.setImageBitmap(event.getBmp());
        holder.textView.setText(event.getInfo());
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView textView;

        ViewHolder(View view){
            super(view);
            imageView = (ImageView)view.findViewById(R.id.list_image_view);
            textView = (TextView) view.findViewById(R.id.list_text_view);
        }
    }
}

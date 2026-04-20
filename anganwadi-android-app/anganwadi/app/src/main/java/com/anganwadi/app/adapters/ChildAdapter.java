package com.anganwadi.app.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.models.Child;
import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.VH> {
    public interface OnChildClick { void onClick(Child child); }
    private List<Child> list;
    private final OnChildClick listener;

    public ChildAdapter(List<Child> list, OnChildClick listener) {
        this.list = list; this.listener = listener;
    }

    public void updateList(List<Child> newList) {
        this.list = newList; notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Child c = list.get(pos);
        h.tvName.setText(c.getName());
        h.tvAge.setText(c.getAgeString() + "  •  " + c.getGender());
        h.tvInitial.setText(String.valueOf(c.getName().charAt(0)));
        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAge, tvInitial;
        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvChildName);
            tvAge     = v.findViewById(R.id.tvChildAge);
            tvInitial = v.findViewById(R.id.tvInitial);
        }
    }
}

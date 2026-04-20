package com.anganwadi.app.adapters;

import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.models.Child;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttendanceToggleAdapter extends RecyclerView.Adapter<AttendanceToggleAdapter.VH> {
    
    public interface OnStatusToggle { void onToggle(Child child, boolean isPresent); }
    
    private List<Child> list;
    private final Set<Integer> presentIds;
    private final OnStatusToggle listener;

    public AttendanceToggleAdapter(List<Child> list, List<Integer> initiallyPresent, OnStatusToggle listener) {
        this.list = list;
        this.presentIds = new HashSet<>(initiallyPresent);
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_toggle, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Child c = list.get(pos);
        h.tvName.setText(c.getName());
        h.tvDetails.setText(c.getGender() + "  •  " + c.getAgeString());
        h.tvInitial.setText(String.valueOf(c.getName().charAt(0)));
        
        boolean isPresent = presentIds.contains(c.getId());
        updateStatusUI(h.ivStatus, isPresent);

        h.itemView.setOnClickListener(v -> {
            boolean currentlyPresent = presentIds.contains(c.getId());
            boolean newState = !currentlyPresent;
            
            if (newState) presentIds.add(c.getId());
            else presentIds.remove(c.getId());
            
            updateStatusUI(h.ivStatus, newState);
            listener.onToggle(c, newState);
        });
    }

    private void updateStatusUI(ImageView iv, boolean isPresent) {
        if (isPresent) {
            iv.setImageResource(R.drawable.ic_tick_green);
        } else {
            iv.setImageResource(R.drawable.ic_cross_red);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvInitial;
        ImageView ivStatus;
        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvChildName);
            tvDetails = v.findViewById(R.id.tvChildDetails);
            tvInitial = v.findViewById(R.id.tvInitial);
            ivStatus  = v.findViewById(R.id.ivStatusIcon);
        }
    }
}
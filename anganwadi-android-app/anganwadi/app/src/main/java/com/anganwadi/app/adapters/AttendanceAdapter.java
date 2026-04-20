package com.anganwadi.app.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.models.Attendance;
import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {
    private final List<Attendance> list;
    public AttendanceAdapter(List<Attendance> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Attendance a = list.get(pos);
        h.tvDate.setText(a.getDate());
        h.tvStatus.setText(a.getStatus().substring(0,1).toUpperCase() + a.getStatus().substring(1));
        int color = a.getStatus().equals("present")
                ? Color.parseColor("#27AE60") : Color.parseColor("#E74C3C");
        h.tvStatus.setTextColor(color);
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatus;
        VH(View v) { super(v); tvDate = v.findViewById(R.id.tvDate); tvStatus = v.findViewById(R.id.tvStatus); }
    }
}

package com.anganwadi.app.adapters;

import android.graphics.Color;
import android.os.AsyncTask;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Vaccination;
import com.anganwadi.app.utils.SMSNotificationManager;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VaccinationAdapter extends RecyclerView.Adapter<VaccinationAdapter.VH> {

    public interface OnVaccinationUpdated { void onUpdated(); }

    private final List<Vaccination> list;
    private final Context context;
    private final DatabaseHelper db;
    private final boolean isWorker;   // workers see Mark Given button, parents don't
    private final int childId;
    private final OnVaccinationUpdated callback;

    // Worker constructor — shows Mark Given button
    public VaccinationAdapter(Context ctx, List<Vaccination> list,
                               DatabaseHelper db, int childId,
                               OnVaccinationUpdated callback) {
        this.context  = ctx;
        this.list     = list;
        this.db       = db;
        this.childId  = childId;
        this.isWorker = true;
        this.callback = callback;
    }

    // Parent constructor — read only, no button
    public VaccinationAdapter(List<Vaccination> list) {
        this.list     = list;
        this.context  = null;
        this.db       = null;
        this.childId  = -1;
        this.isWorker = false;
        this.callback = null;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vaccination, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Vaccination v = list.get(pos);
        h.tvName.setText(v.getVaccineName());
        h.tvDue.setText("Due: " + v.getDueDate());

        // ── Status badge ─────────────────────────────────────
        switch (v.getStatus()) {
            case "given":
                h.tvStatus.setText("\u2713 Given");
                h.tvStatus.setTextColor(Color.parseColor("#27AE60"));
                h.tvStatus.setBackgroundResource(R.drawable.badge_green);
                if (v.getGivenDate() != null && !v.getGivenDate().isEmpty())
                    h.tvDue.setText("Given on: " + v.getGivenDate());
                if (isWorker) h.btnMarkGiven.setVisibility(View.GONE);
                break;
            case "missed":
                h.tvStatus.setText("\u2717 Missed");
                h.tvStatus.setTextColor(Color.parseColor("#C0392B"));
                h.tvStatus.setBackgroundResource(R.drawable.badge_red);
                if (isWorker) h.btnMarkGiven.setVisibility(View.VISIBLE);
                break;
            default: // pending
                h.tvStatus.setText("\u23F3 Pending");
                h.tvStatus.setTextColor(Color.parseColor("#D68910"));
                h.tvStatus.setBackgroundResource(R.drawable.badge_orange);
                if (isWorker) h.btnMarkGiven.setVisibility(View.VISIBLE);
                break;
        }

        // ── Mark Given button — only for workers ─────────────
        if (!isWorker) {
            h.btnMarkGiven.setVisibility(View.GONE);
            return;
        }

        h.btnMarkGiven.setOnClickListener(view -> {
            String today = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault()).format(new Date());

            // Confirm dialog
            new androidx.appcompat.app.AlertDialog.Builder(view.getContext())
                .setTitle("Mark Vaccination Given")
                .setMessage("Mark " + v.getVaccineName() + " as given today (" + today + ")?")
                .setPositiveButton("Yes, Mark Given", (dialog, which) -> {
                    new MarkVaccinationTask(v, today, pos).execute();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    @Override public int getItemCount() { return list.size(); }

    // ── Background task to update vaccination status ──────────
    private class MarkVaccinationTask extends AsyncTask<Void, Void, Boolean> {
        final Vaccination vacc;
        final String givenDate;
        final int position;

        MarkVaccinationTask(Vaccination v, String date, int pos) {
            this.vacc = v; this.givenDate = date; this.position = pos;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return db.markVaccinationGiven(vacc.getId(), givenDate);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success || context == null) return;

            // Update model in-place
            vacc.setStatus("given");
            vacc.setGivenDate(givenDate);
            notifyItemChanged(position);

            Toast.makeText(context,
                    vacc.getVaccineName() + " marked as given!",
                    Toast.LENGTH_SHORT).show();

            // Auto send SMS to parent
            String sms = SMSNotificationManager.buildVaccinationGivenMessage(
                    "Child", vacc.getVaccineName(), givenDate);
            String phone = db.getChildPhone(childId);
            if (phone != null && !phone.isEmpty()) {
                SMSNotificationManager.sendSMS(context, phone, sms, result ->
                    Toast.makeText(context,
                        result ? "SMS sent to parent!" : "SMS failed",
                        Toast.LENGTH_SHORT).show());
            }

            if (callback != null) callback.onUpdated();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDue, tvStatus;
        Button   btnMarkGiven;

        VH(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvVaccineName);
            tvDue       = v.findViewById(R.id.tvDueDate);
            tvStatus    = v.findViewById(R.id.tvStatus);
            btnMarkGiven= v.findViewById(R.id.btnMarkGiven);
        }
    }
}

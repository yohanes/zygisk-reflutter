package com.tinyhack.zygiskreflutter;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ApplicationInfo item);
    }

    private List<ApplicationInfo> appList;
    private List<ApplicationInfo> origList;
    private OnItemClickListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView appName;


        public ViewHolder(View view) {
            super(view);
            appName = (TextView) view.findViewById(R.id.app_name);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Call the onItemClick method of the listener
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(appList.get(position));
                        }
                    }
                }
            });
        }
        public TextView getAppName() {
            return appName;
        }
    }

    private PackageManager pm;

    public AppListAdapter(List<ApplicationInfo> appList, PackageManager pm, OnItemClickListener listener) {
        this.appList = appList;
        this.origList = new ArrayList<>(appList);
        this.pm = pm;
        this.listener = listener;
    }

    @Override
    public AppListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.getAppName().setText(appList.get(position).loadLabel(pm).toString());
    }

    private String lastQuery = null;

    public void filter(String text) {
        List<ApplicationInfo> listToUse = origList;
        if (lastQuery != null && text.startsWith(lastQuery)) {
            listToUse = appList;
            lastQuery = text;
        } else {
            lastQuery = text;
        }
        List<ApplicationInfo> filteredList = new ArrayList<>();
        //filter original list
        for (ApplicationInfo item : listToUse) {
            String label = pm.getApplicationLabel(item).toString();

            //if (item.loadLabel(pm).toString().toLowerCase().contains(text.toLowerCase())) {
            if (label.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        appList = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }
}

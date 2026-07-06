package com.sokoban.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {

    private final List<String> levelFiles;
    private final Consumer<Integer> onLevelClick;

    public LevelAdapter(List<String> levelFiles, Consumer<Integer> onLevelClick) {
        this.levelFiles = levelFiles;
        this.onLevelClick = onLevelClick;
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_level, parent, false);
        return new LevelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        String fileName = levelFiles.get(position);
        
        // Remove .json extension
        String displayName = fileName.replace(".json", "");
        
        // Remove spaces
        displayName = displayName.replaceAll("\\s+", "");
        
        // Remove leading zeroes
        displayName = displayName.replaceFirst("^0+(?!$)", "");
        
        holder.levelNumberText.setText(displayName);
        
        holder.itemView.setOnClickListener(v -> onLevelClick.accept(position));
    }

    @Override
    public int getItemCount() {
        return levelFiles == null ? 0 : levelFiles.size();
    }

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        TextView levelNumberText;

        public LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            levelNumberText = itemView.findViewById(R.id.levelNumberText);
        }
    }
}

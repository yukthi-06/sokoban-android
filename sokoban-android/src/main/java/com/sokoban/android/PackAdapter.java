package com.vypeensoft.sokoban.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class PackAdapter extends RecyclerView.Adapter<PackAdapter.PackViewHolder> {

    private final List<String> packNames;
    private final java.util.Map<String, Integer> packCounts;
    private final Consumer<String> onPackClick;

    public PackAdapter(List<String> packNames, java.util.Map<String, Integer> packCounts, Consumer<String> onPackClick) {
        this.packNames = packNames;
        this.packCounts = packCounts;
        this.onPackClick = onPackClick;
    }

    @NonNull
    @Override
    public PackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_level, parent, false);
        return new PackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackViewHolder holder, int position) {
        String packName = packNames.get(position);
        int count = packCounts.getOrDefault(packName, 0);
        
        holder.levelNumberText.setText(packName + "\n(" + count + ")");
        holder.levelNumberText.setTextSize(18f); // Make text smaller to fit Pack Name
        holder.tickMarkText.setVisibility(View.GONE); // No tick mark for packs yet
        
        holder.levelCard.setOnClickListener(v -> onPackClick.accept(packName));
    }

    @Override
    public int getItemCount() {
        return packNames == null ? 0 : packNames.size();
    }

    static class PackViewHolder extends RecyclerView.ViewHolder {
        TextView levelNumberText;
        TextView tickMarkText;
        View levelCard;

        public PackViewHolder(@NonNull View itemView) {
            super(itemView);
            levelNumberText = itemView.findViewById(R.id.levelNumberText);
            tickMarkText = itemView.findViewById(R.id.tickMarkText);
            levelCard = itemView.findViewById(R.id.levelCard);
        }
    }
}

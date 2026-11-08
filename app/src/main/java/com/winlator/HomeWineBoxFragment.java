package com.winlator;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HomeWineBoxFragment extends Fragment {
    private static final String[] GAME_NAMES = {
        "Hollow Knight", "Stardew Valley", "Portal 2", "Celeste", "Terraria"
    };
    private static final String[] GAME_GLYPHS = {"♞", "🌱", "◈", "✦", "▦"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_winebox, container, false);

        RecyclerView games = root.findViewById(R.id.RVWineBoxGames);
        games.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        games.setAdapter(new GamesAdapter());

        root.findViewById(R.id.chipLastGame).setOnClickListener(v -> openShortcuts());
        root.findViewById(R.id.chipAddGame).setOnClickListener(v -> openShortcuts());
        root.findViewById(R.id.chipCustomize).setOnClickListener(v -> openSettings());
        root.findViewById(R.id.cardStatistics).setOnClickListener(v -> openContainers());
        root.findViewById(R.id.cardNews).setOnClickListener(v -> openShortcuts());

        return root;
    }

    private void openShortcuts() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity)getActivity()).showFragment(new ShortcutsFragment());
        }
    }

    private void openSettings() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity)getActivity()).showFragment(new SettingsFragment());
        }
    }

    private void openContainers() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity)getActivity()).showFragment(new ContainersFragment());
        }
    }

    private class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.GameHolder> {
        private int selectedPosition = 0;

        class GameHolder extends RecyclerView.ViewHolder {
            final View iconFrame;
            final TextView icon;
            final TextView name;

            GameHolder(View itemView) {
                super(itemView);
                iconFrame = itemView.findViewById(R.id.gameIconFrame);
                icon = itemView.findViewById(R.id.gameIcon);
                name = itemView.findViewById(R.id.gameName);
            }
        }

        @NonNull
        @Override
        public GameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_winebox_game_item, parent, false);
            return new GameHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull GameHolder holder, int position) {
            final int itemPosition = position;
            boolean selected = itemPosition == selectedPosition;
            holder.icon.setText(GAME_GLYPHS[itemPosition]);
            holder.name.setText(GAME_NAMES[itemPosition]);
            holder.name.setVisibility(selected ? View.VISIBLE : View.GONE);

            ViewGroup.LayoutParams params = holder.iconFrame.getLayoutParams();
            int size = dp(selected ? 96 : 56);
            params.width = size;
            params.height = size;
            holder.iconFrame.setLayoutParams(params);

            GradientDrawable tile = new GradientDrawable();
            tile.setColor(Color.parseColor(selected ? "#202838" : "#1A1F2B"));
            tile.setCornerRadius(dp(12));
            if (selected) tile.setStroke(dp(2), Color.WHITE);
            holder.iconFrame.setBackground(tile);

            holder.itemView.setScaleX(1f);
            holder.itemView.setScaleY(1f);
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.1f : 1f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
            });
            holder.itemView.setOnClickListener(v -> {
                int oldPosition = selectedPosition;
                selectedPosition = itemPosition;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return GAME_NAMES.length;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

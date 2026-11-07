package com.appletv.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, zero-bloat visual wallpaper picker adapter for tvOS 18 grid.
 *
 * Memory Guardrails:
 * - Decodes thumbnails strictly down to 320x180 px in Bitmap.Config.RGB_565 (~115 KB/image).
 * - Capped in an in-memory LruCache (max 4 MB).
 * - Background Executor with low priority to prevent UI thread drops.
 * - Complete teardown and eviction on cleanup().
 */
public class WallpaperPickerAdapter extends RecyclerView.Adapter<WallpaperPickerAdapter.WallpaperViewHolder> {

    public interface OnWallpaperActionListener {
        void onWallpaperFocused(File file, int position);
        void onWallpaperSelected(File file, int position);
    }

    private final List<File> wallpapers = new ArrayList<>();
    private final String activeWallpaperName;
    private final OnWallpaperActionListener listener;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService decodeExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isCleanedUp = false;

    public WallpaperPickerAdapter(List<File> list, String activeName, OnWallpaperActionListener listener) {
        if (list != null) {
            this.wallpapers.addAll(list);
        }
        this.activeWallpaperName = activeName;
        this.listener = listener;

        // Bounded 4 MB memory cache for 16:9 mini-thumbnails (11 images ~ 1.26 MB)
        final int maxCacheSize = 4 * 1024 * 1024;
        this.memoryCache = new LruCache<String, Bitmap>(maxCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };

        this.decodeExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "tvos-thumb-loader");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallpaper_card, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        File file = wallpapers.get(position);
        holder.bind(file, position);
    }

    @Override
    public int getItemCount() {
        return wallpapers.size();
    }

    public int getPositionForFile(File target) {
        if (target == null) return -1;
        for (int i = 0; i < wallpapers.size(); i++) {
            if (wallpapers.get(i).getName().equalsIgnoreCase(target.getName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Reclaims 100% of thumbnail memory when the visual picker dialog is dismissed.
     */
    public void cleanup() {
        isCleanedUp = true;
        if (decodeExecutor != null && !decodeExecutor.isShutdown()) {
            decodeExecutor.shutdownNow();
        }
        if (memoryCache != null) {
            memoryCache.evictAll();
        }
        wallpapers.clear();
        System.gc();
    }

    static String formatDisplayName(String filename) {
        String base = filename.replaceAll("\\.[^.]+$", "");
        base = base.replaceAll("-[0-9]+x[0-9]+.*$", "");
        base = base.replace('-', ' ').replace('_', ' ');
        String[] words = base.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) sb.append(w.substring(1).toLowerCase());
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private void loadThumbnailAsync(File file, ImageView imageView) {
        final String path = file.getPath();
        imageView.setTag(path);

        Bitmap cached = memoryCache.get(path);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageBitmap(cached);
            imageView.setAlpha(1.0f);
            return;
        }

        imageView.setImageBitmap(null);
        if (isCleanedUp) return;

        decodeExecutor.execute(() -> {
            if (isCleanedUp) return;
            Bitmap thumb = decodeSampledBitmap(path, 320, 180);
            if (thumb != null && !isCleanedUp) {
                memoryCache.put(path, thumb);
                mainHandler.post(() -> {
                    if (isCleanedUp) return;
                    if (path.equals(imageView.getTag())) {
                        imageView.setImageBitmap(thumb);
                        imageView.setAlpha(0f);
                        imageView.animate().alpha(1f).setDuration(160).start();
                    }
                });
            }
        });
    }

    private static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);
            int w = opts.outWidth, h = opts.outHeight;
            if (w <= 0 || h <= 0) return null;

            int sample = 1;
            while ((w / (sample * 2)) >= reqWidth && (h / (sample * 2)) >= reqHeight) {
                sample *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            Bitmap sampled = BitmapFactory.decodeFile(path, opts);
            if (sampled == null) return null;

            if (sampled.getWidth() > reqWidth * 1.4f || sampled.getHeight() > reqHeight * 1.4f) {
                Bitmap scaled = Bitmap.createScaledBitmap(sampled, reqWidth, reqHeight, true);
                if (scaled != sampled) {
                    sampled.recycle();
                }
                return scaled;
            }
            return sampled;
        } catch (Throwable t) {
            return null;
        }
    }

    class WallpaperViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout cardHolder;
        final FrameLayout cardContainer;
        final ImageView thumbnail;
        final View focusRing;
        final TextView badgeActive;
        final TextView cardTitle;

        WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHolder = itemView.findViewById(R.id.cardHolder);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            focusRing = itemView.findViewById(R.id.focusRing);
            badgeActive = itemView.findViewById(R.id.badgeActive);
            cardTitle = itemView.findViewById(R.id.cardTitle);

            // Hardware-accelerated rounded rectangle clipping
            cardContainer.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            cardContainer.setClipToOutline(true);
        }

        void bind(File file, int position) {
            String displayName = formatDisplayName(file.getName());
            cardTitle.setText(displayName);

            boolean isActive = activeWallpaperName != null && activeWallpaperName.equalsIgnoreCase(file.getName());
            badgeActive.setVisibility(isActive ? View.VISIBLE : View.GONE);

            loadThumbnailAsync(file, thumbnail);

            cardHolder.setOnFocusChangeListener((v, hasFocus) -> {
                focusRing.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
                cardHolder.animate()
                        .scaleX(hasFocus ? 1.12f : 1.0f)
                        .scaleY(hasFocus ? 1.12f : 1.0f)
                        .translationZ(hasFocus ? 16f : 0f)
                        .setDuration(160)
                        .start();
                cardHolder.setAlpha(hasFocus ? 1.0f : 0.68f);

                if (hasFocus && listener != null) {
                    listener.onWallpaperFocused(file, position);
                }
            });

            cardHolder.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWallpaperSelected(file, position);
                }
            });

            // Initial state: slightly dimmed unless focused
            cardHolder.setAlpha(cardHolder.hasFocus() ? 1.0f : 0.68f);
        }
    }
}

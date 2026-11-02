package com.appletv.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the tvOS style glass platter behind the first (dock) row and adds offsets for the label. */
public class DockDecoration extends RecyclerView.ItemDecoration {

    static final float PAD_H_DP = 12f; // 24px inner horizontal padding (spec: 20-28px)
    static final float PAD_V_DP = 10f; // 20px inner vertical padding (spec: 16-24px)
    static final float RADIUS_DP = 14f; // 28px corner radius (spec: 24-32px)

    private final int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    DockDecoration(int dockSize, float density) {
        this.dockSize = dockSize;
        this.density = density;
        band.setColor(0xB8121214);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(1.0f * density);
        edge.setColor(0x2EFFFFFF);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (PAD_V_DP * density);
            outRect.bottom = (int) ((PAD_V_DP + 16f) * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        View firstChild = null;
        View lastChild = null;
        int minPos = Integer.MAX_VALUE;
        int maxPos = Integer.MIN_VALUE;

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            if (child.getVisibility() != View.VISIBLE) continue;
            View card = child.findViewById(R.id.card);
            if (card == null || card.getVisibility() != View.VISIBLE) continue;

            if (pos < minPos) {
                minPos = pos;
                firstChild = child;
            }
            if (pos > maxPos) {
                maxPos = pos;
                lastChild = child;
            }
        }
        if (firstChild == null || lastChild == null) return;

        View firstCard = firstChild.findViewById(R.id.card);
        View lastCard = lastChild.findViewById(R.id.card);
        if (firstCard == null || lastCard == null) return;

        float cardW = firstCard.getWidth();
        float cardH = firstCard.getHeight();
        if (cardW <= 0 || cardH <= 0) return;

        // Unhovered layout coordinates in parent space
        float card0Left = firstChild.getLeft() + firstCard.getLeft();
        float cardLastRight = lastChild.getLeft() + lastCard.getRight();
        float cardTop = firstChild.getTop() + firstCard.getTop();
        float cardBottom = cardTop + cardH;

        float padH = PAD_H_DP * density;
        float padV = PAD_V_DP * density;

        // Dock platter covers the icons with standard inner padding
        float l = card0Left - padH;
        float r = cardLastRight + padH;
        float t = cardTop - padV;
        float b = cardBottom + padV;

        rect.set(l, t, r, b);
        float radius = RADIUS_DP * density;
        c.drawRoundRect(rect, radius, radius, band);
        c.drawRoundRect(rect, radius, radius, edge);
    }
}

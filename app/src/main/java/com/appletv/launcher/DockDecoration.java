package com.appletv.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the tvOS style lighter band behind the first (dock) row and adds a gap below it. */
public class DockDecoration extends RecyclerView.ItemDecoration {

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
            outRect.top = (int) (12 * density);
            outRect.bottom = (int) (18 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean any = false;
        float minT = Float.MAX_VALUE;
        float maxB = Float.MIN_VALUE;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            View card = child.findViewById(R.id.card);
            if (card == null) continue;
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float b = t + card.getHeight();
            minT = Math.min(minT, t);
            maxB = Math.max(maxB, b);
            any = true;
        }
        if (!any) return;

        float l = parent.getPaddingLeft() - (12 * density);
        float r = parent.getWidth() - parent.getPaddingRight() + (12 * density);
        float padY = 20 * density;
        float t = minT - padY;
        float b = maxB + padY;

        rect.set(l, t, r, b);
        float radius = 28 * density;
        c.drawRoundRect(rect, radius, radius, band);
        c.drawRoundRect(rect, radius, radius, edge);
    }
}

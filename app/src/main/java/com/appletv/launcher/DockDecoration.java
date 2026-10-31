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
        View firstChild = null;
        View secondChild = null;
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

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos == minPos + 1 && child.getVisibility() == View.VISIBLE) {
                secondChild = child;
                break;
            }
        }

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

        float gap;
        if (secondChild != null) {
            View secondCard = secondChild.findViewById(R.id.card);
            float card1Left = secondChild.getLeft() + secondCard.getLeft();
            float card0Right = card0Left + cardW;
            gap = card1Left - card0Right;
        } else {
            gap = 26f * density;
        }
        if (gap <= 0) {
            gap = 26f * density;
        }

        // Distance(Dock Left -> Card 0) = gap
        // Distance(Card Last -> Dock Right) = gap
        // Distance(Dock Top -> Card Top) = gap
        // Distance(Card Bottom -> Dock Bottom) = gap
        float l = card0Left - gap;
        float r = cardLastRight + gap;
        float t = cardTop - gap;
        float b = cardBottom + gap;

        rect.set(l, t, r, b);
        float cardRadius = AppAdapter.cornerRadius((int) cardW, density);
        float radius = cardRadius + gap * 0.5f;
        c.drawRoundRect(rect, radius, radius, band);
        c.drawRoundRect(rect, radius, radius, edge);
    }
}

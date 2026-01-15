package com.sandg.tastebuds

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Adds vertical spacing (in pixels) between items in a vertical list.
 */
class SpacingItemDecoration(private val verticalSpacingPx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        if (position > 0) {
            outRect.top = verticalSpacingPx
        }
    }
}

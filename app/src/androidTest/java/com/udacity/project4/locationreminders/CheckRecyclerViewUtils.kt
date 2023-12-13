package com.udacity.project4.locationreminders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

object CheckRecyclerViewUtils {
    // Check at a specific position of the RecyclerView
    fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                if (view !is RecyclerView) {
                    return false
                }

                val viewHolder = view.findViewHolderForAdapterPosition(position)
                return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}

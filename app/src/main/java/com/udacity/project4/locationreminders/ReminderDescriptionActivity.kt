package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderDescriptionBinding

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        // Receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = R.layout.activity_reminder_description
        binding = DataBindingUtil.setContentView(this, layoutId)
        val reminderDataItem =
            intent.getSerializableExtra(EXTRA_ReminderDataItem) as? ReminderDataItem
        if (reminderDataItem != null) {
            binding.reminderDataItem = reminderDataItem

            binding.googleLink.setOnClickListener {
                val latitude = String.format("%.4f", reminderDataItem.latitude)
                val longitude = String.format("%.4f", reminderDataItem.longitude)
                val locationName = reminderDataItem.location
                val zoomLevel = 15
                val location = "geo:$latitude,$longitude?z$zoomLevel&q=$latitude,$longitude($locationName)"

                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(location))

                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps?q=$latitude,$longitude"),
                    )
                    startActivity(webIntent)
                }
            }
        }
    }
}

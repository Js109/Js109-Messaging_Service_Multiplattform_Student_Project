package de.uulm.automotiveuulmapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import de.uulm.automotive.cds.entities.MessageSerializable
import java.net.URL

class MessageContentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_content)

        val message = intent.getSerializableExtra("message") as MessageSerializable
        val titleView = findViewById<TextView>(R.id.messageContentTitleText)
        val messageContentView = findViewById<TextView>(R.id.messageContentText)

        titleView.setText(message.title)
        messageContentView.setText(message.messageText)
        // if attachment exists, decode base64-encoded byte array
        val image = message.attachment
        if(image != null && image.isNotEmpty()) {
            val bmp =
                BitmapFactory.decodeByteArray(message.attachment, 0, message.attachment!!.size)

            val imageView = findViewById<ImageView>(R.id.messageContentImageView)
            imageView.setImageBitmap(bmp)
        } else {
            findViewById<ImageView>(R.id.messageContentImageView).visibility = View.GONE
        }

        val map =
            supportFragmentManager.findFragmentById(R.id.message_map) as SupportMapFragment

        val mapsUrl = message.links?.firstOrNull { isGoogleMapsUrl(it) }
        mapsUrl?.let {
            getCoordinatesFromMapsUrl(it)
        }?.let { coords ->
            map.getMapAsync { map ->
                val location = LatLng(coords.first, coords.second)
                val marker = map.addMarker(MarkerOptions().position(location).title(message.title))
                marker.showInfoWindow()
                map.moveCamera(CameraUpdateFactory.newLatLng(location))
                map.moveCamera(CameraUpdateFactory.zoomTo(17.0f))
            }

        } ?: run {
            findViewById<ConstraintLayout>(R.id.message_map_container).visibility = View.GONE
        }

    }

    private fun isGoogleMapsUrl(url: URL): Boolean {
        if (!(url.host == "google.com" || url.host == "www.google.com"))
            return false
        val pathSplit = url.path.split("/").filter { it.isNotEmpty() }
        if (pathSplit.isEmpty())
            return false
        return pathSplit[0] == "maps"
    }

    private fun getCoordinatesFromMapsUrl(url: URL): Pair<Double, Double>? {
        val pathSplit = url.path.split("/")
        val coords = pathSplit.first { it.startsWith("@") }
        val coordsSplit = coords.removeRange(0..0).split(",")
        return if (coordsSplit.size < 2) {
            null
        } else {
            Pair(coordsSplit[0].toDouble(), coordsSplit[1].toDouble())
        }
    }
}

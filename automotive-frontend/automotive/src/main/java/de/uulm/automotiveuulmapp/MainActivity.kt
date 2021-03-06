package de.uulm.automotiveuulmapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.uulm.automotiveuulmapp.messages.messageFragment.MessageFragment
import de.uulm.automotiveuulmapp.locationFavourites.LocationFavouritesFragment
import de.uulm.automotiveuulmapp.messages.messagedb.MessageDatabase
import de.uulm.automotiveuulmapp.topicFragment.TopicFragment

class MainActivity : AppCompatActivity(){
    companion object {
        const val REQUEST_LOCATION_PERMISSIONS_CODE = 1
    }

    lateinit var hasNewMessagesLiveData: LiveData<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val navBar = findViewById<BottomNavigationView>(R.id.bottomNavigationBar)
        setHeadBar(R.string.header_bar_subscription_title)
        loadFragment(TopicFragment())

        navBar.setOnNavigationItemSelectedListener{item ->
            var titleResourceId: Int? = null
            val fragment = when(item.itemId) {
                R.id.nav_item_messages -> {
                    titleResourceId = R.string.header_bar_message_view_title
                    MessageFragment()
                }
                R.id.nav_item_locations -> {
                    titleResourceId = R.string.header_bar_location_favourites_title
                    LocationFavouritesFragment()
                }
                R.id.nav_item_subscriptions -> {
                    titleResourceId = R.string.header_bar_subscription_title
                    TopicFragment()
                }
                else -> {
                    null
                }
            }
            setHeadBar(titleResourceId!!)
            loadFragment(fragment)
        }

        navBar.setOnNavigationItemReselectedListener {
            //do nothing when reselected
        }

        hasNewMessagesLiveData = MessageDatabase.getDaoInstance(this).newMessageCount()
        hasNewMessagesLiveData.observeForever { count ->
            val newMessagesSymbol = findViewById<TextView>(R.id.newMessagesSymbol)
            newMessagesSymbol.visibility = if (count > 0) View.VISIBLE else View.INVISIBLE
        }

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSIONS_CODE
            )
        }
    }

    private fun setHeadBar(title: Int) {
        findViewById<Toolbar>(R.id.header_bar).setTitle(title)
    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        //switching fragment
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            return true
        }
        return false
    }
}

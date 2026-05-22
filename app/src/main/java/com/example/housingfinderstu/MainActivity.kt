package com.example.housingfinderstu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Use Room database directly
    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: HouseAdapter

    private var currentUserId = -1L
    private var currentUserPrefLocation = ""
    private var currentUserPrefMaxPrice = 5000
    private var selectedHouse: HouseEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Room database
        db = AppDatabase.getInstance(this)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getLong("current_user_id", -1)

        // Check login
        if (currentUserId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Pre-populate 50 houses if needed
        lifecycleScope.launch {
            val count = db.houseDao().getAllHouses().first().size
            if (count < 50) {
                prePopulateHouses()
            }
        }

        loadUserPreferences()
        setupRecyclerView()
        setupFilters()
        loadHouses()
        createNotificationChannel()

        // Logout button
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit().remove("current_user_id").apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Navigation button (Extension Feature)
        findViewById<Button>(R.id.btnNavigation).setOnClickListener {
            if (selectedHouse != null) {
                val query = Uri.encode("${selectedHouse!!.location}, Gaborone, Botswana")
                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } else {
                Toast.makeText(this, "Please tap a house first", Toast.LENGTH_SHORT).show()
            }
        }

        // Profile button
        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private suspend fun prePopulateHouses() {
        val locations = listOf("Gaborone West", "Gaborone North", "Broadhurst", "Village", "Phase 4", "CBD")
        val types = listOf("Single Room", "Apartment", "Studio", "Shared Room", "House")
        val imageNames = listOf("house1", "house2", "house3", "house4", "house5", "house6", "house7", "house8")

        for (i in 1..50) {
            val house = HouseEntity(
                title = "Modern Room $i in Gaborone",
                price = 1500 + (i * 100),
                location = locations[i % locations.size],
                type = types[i % types.size],
                amenities = "WiFi,Parking,Water",
                availability_date = "2026-06-${(i % 28) + 1}",
                deposit = 1000,
                is_reserved = 0,
                reserved_by = -1,
                imageName = imageNames[i % imageNames.size]
            )
            db.houseDao().insertHouse(house)
        }
    }

    private fun loadUserPreferences() {
        lifecycleScope.launch {
            val user = db.userDao().getUserById(currentUserId)
            user?.let {
                currentUserPrefLocation = it.prefLocation ?: ""
                currentUserPrefMaxPrice = it.prefMaxPrice ?: 5000
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HouseAdapter(emptyList()) { house ->
            selectedHouse = house
            val intent = Intent(this, ReservationActivity::class.java)
            intent.putExtra("house_id", house.id)
            startActivity(intent)
        }

        findViewById<RecyclerView>(R.id.rvHouses).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupFilters() {
        val etMinPrice = findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice = findViewById<EditText>(R.id.etMaxPrice)
        val spinnerLocation = findViewById<Spinner>(R.id.spinnerLocation)
        val etDate = findViewById<EditText>(R.id.etAvailabilityDate)
        val btnFilter = findViewById<Button>(R.id.btnFilter)
        val btnClear = findViewById<Button>(R.id.btnClearFilter)

        val locations = listOf("All Areas", "Gaborone West", "Gaborone North", "Broadhurst", "Village", "Phase 4", "CBD")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = spinnerAdapter

        btnFilter.setOnClickListener {
            val minPrice = etMinPrice.text.toString().toIntOrNull() ?: 0
            val maxPrice = etMaxPrice.text.toString().toIntOrNull() ?: 10000
            val location = spinnerLocation.selectedItem.toString()
            val date = etDate.text.toString()

            lifecycleScope.launch {
                val locationFilter = if (location == "All Areas") "" else location
                val filtered = db.houseDao().filterHouses(minPrice, maxPrice, locationFilter, date).first()
                adapter.updateList(filtered)
                checkForAlerts(filtered)
            }
        }

        btnClear.setOnClickListener {
            etMinPrice.text.clear()
            etMaxPrice.text.clear()
            spinnerLocation.setSelection(0)
            etDate.text.clear()
            loadHouses()
        }
    }

    private fun loadHouses() {
        lifecycleScope.launch {
            db.houseDao().getAvailableHouses().collect { houses ->
                adapter.updateList(houses)
            }
        }
    }

    private fun checkForAlerts(houses: List<HouseEntity>) {
        if (currentUserPrefLocation.isNotEmpty() && currentUserPrefLocation != "All Areas") {
            val matching = houses.filter {
                it.location.equals(currentUserPrefLocation, ignoreCase = true) &&
                        it.price <= currentUserPrefMaxPrice
            }
            if (matching.isNotEmpty()) {
                sendNotification("🏠 Housing Alert!", "${matching.size} properties found matching your preferences")
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "housing_alerts")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("housing_alerts", "Housing Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

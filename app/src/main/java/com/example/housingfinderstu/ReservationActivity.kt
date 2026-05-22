package com.example.housingfinderstu

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ReservationActivity - Handles the deposit payment and room reservation process
 */
class ReservationActivity : AppCompatActivity() {

    // Room database instance
    private lateinit var db: AppDatabase

    // SharedPreferences to store user session data
    private lateinit var prefs: SharedPreferences

    // ID of the house being reserved (passed from MainActivity)
    private var houseId: Long = -1

    // ID of the currently logged-in user (from SharedPreferences)
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation)

        // Initialize Room database
        db = AppDatabase.getInstance(this)

        // Get user session data
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getLong("current_user_id", -1)

        // Get the house ID from the intent (passed when user clicked on a house)
        houseId = intent.getLongExtra("house_id", -1)

        // Validate that a valid house was selected
        if (houseId == -1L) {
            Toast.makeText(this, "Error: No house selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Display the house details to the user
        loadHouseDetails()

        // Set up the payment button click listener
        val btnPay = findViewById<Button>(R.id.btnSimulatePayment)
        btnPay.setOnClickListener { simulatePayment() }
    }

    /**
     * Loads and displays all details of the selected house
     * Also checks if the house is already reserved and disables payment if so
     */
    private fun loadHouseDetails() {
        lifecycleScope.launch {
            // Fetch house details from Room database
            val house = db.houseDao().getHouseById(houseId)

            runOnUiThread {
                if (house != null) {
                    // Display all house information to the user
                    findViewById<TextView>(R.id.tvHouseTitle).text = house.title
                    findViewById<TextView>(R.id.tvPrice).text = "Monthly Rent: BWP ${house.price}"
                    findViewById<TextView>(R.id.tvDeposit).text = "Deposit Required: BWP ${house.deposit}"
                    findViewById<TextView>(R.id.tvLocation).text = "📍 Location: ${house.location}"
                    findViewById<TextView>(R.id.tvType).text = "🏠 Type: ${house.type}"
                    findViewById<TextView>(R.id.tvAmenities).text = "✨ Amenities: ${house.amenities}"
                    findViewById<TextView>(R.id.tvAvailable).text = "📅 Available from: ${house.availability_date}"

                    // ===== EXTENSION FEATURE: Campus Distance =====
                    // Calculate and display distance to campus
                    val distanceText = calculateDistanceToCampus(house.location)
                    findViewById<TextView>(R.id.tvCampusDistance).text = distanceText

                    // Check if house is already reserved (prevents double reservation)
                    if (house.is_reserved == 1) {
                        // Disable payment button and show status
                        findViewById<Button>(R.id.btnSimulatePayment).isEnabled = false
                        findViewById<Button>(R.id.btnSimulatePayment).text = "Already Reserved"
                        findViewById<TextView>(R.id.tvReceipt).text = "❌ This property has already been reserved by another student."
                    }
                } else {
                    // House not found in database
                    Toast.makeText(this@ReservationActivity, "House not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    /**
     * ===== EXTENSION FEATURE: Campus Distance  =====
     * Calculates distance from house location to University of Botswana campus
     * This fulfills the "Campus distance & route navigation" extension requirement
     */
    private fun calculateDistanceToCampus(location: String): String {
        // Hardcoded distances for Gaborone areas (in kilometers from UB Main Campus)
        val distances = mapOf(
            "Gaborone West" to 5.2,
            "Gaborone North" to 3.8,
            "Gaborone South" to 7.5,
            "Broadhurst" to 2.1,
            "Village" to 1.5,
            "Maruapula" to 4.0,
            "Phase 2" to 3.2,
            "Phase 4" to 3.5,
            "Block 3" to 6.0,
            "Block 5" to 5.5,
            "CBD" to 2.5,
            "Tlokweng" to 8.0
        )

        val distance = distances[location] ?: 0.0
        val travelTime = (distance / 15 * 60).toInt() // 15 km/h average speed = minutes

        return "🏫 Distance to UB Campus: ${String.format("%.1f", distance)} km (~$travelTime min drive)"
    }

    /**
     * Simulates the payment process and reserves the room
     * Requirements:
     * 1. Simulated payment workflow
     * 2. Generate unique reference number
     * 3. Change listing status to 'Reserved'
     * 4. Prevent double booking
     */
    private fun simulatePayment() {
        lifecycleScope.launch {
            // Get the house details again to ensure it's still available
            val house = db.houseDao().getHouseById(houseId)

            // Validation checks before processing payment
            if (house == null) {
                runOnUiThread {
                    Toast.makeText(this@ReservationActivity, "House not found", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // DOUBLE-CHECK: Prevent double booking
            if (house.is_reserved == 1) {
                runOnUiThread {
                    Toast.makeText(this@ReservationActivity, "This house is already reserved!", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // ===== SIMULATED PAYMENT WORKFLOW  =====
            runOnUiThread {
                Toast.makeText(this@ReservationActivity, "Processing payment...", Toast.LENGTH_SHORT).show()
            }

            // ===== GENERATE UNIQUE REFERENCE NUMBER =====
            // Format: HFS-XXXXXXXX (e.g., HFS-A3F9B2E1)
            val referenceNumber = "HFS-${UUID.randomUUID().toString().take(8).uppercase()}"
            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // ===== RESERVE THE HOUSE  =====
            // Update house status to reserved
            val updatedHouse = house.copy(
                is_reserved = 1,
                reserved_by = currentUserId
            )
            db.houseDao().updateHouse(updatedHouse)

            // Create reservation record
            val reservation = ReservationEntity(
                user_id = currentUserId,
                house_id = houseId,
                reference_number = referenceNumber,
                deposit_paid = house.deposit,
                date = currentDate
            )
            db.reservationDao().insertReservation(reservation)

            runOnUiThread {
                // ===== DISPLAY RECEIPT  =====
                val receipt = """
                    ═══════════════════════════════
                       PAYMENT RECEIPT
                    ═══════════════════════════════
                    
                    Reference: $referenceNumber
                    Property: ${house.title}
                    Location: ${house.location}
                    Deposit Paid: BWP ${house.deposit}
                    Student ID: $currentUserId
                    Status: ✅ RESERVED
                    
                    Date: $currentDate
                    
                    ═══════════════════════════════
                    Thank you for your reservation!
                    ═══════════════════════════════
                """.trimIndent()

                // Display the receipt in the TextView
                findViewById<TextView>(R.id.tvReceipt).text = receipt

                // Disable the payment button to prevent multiple reservations
                findViewById<Button>(R.id.btnSimulatePayment).isEnabled = false
                findViewById<Button>(R.id.btnSimulatePayment).text = "✓ Reserved Successfully"

                // Show success message with reference number
                Toast.makeText(this@ReservationActivity,
                    "✅ Payment successful! Your reference: $referenceNumber",
                    Toast.LENGTH_LONG).show()
            }
        }
    }
}

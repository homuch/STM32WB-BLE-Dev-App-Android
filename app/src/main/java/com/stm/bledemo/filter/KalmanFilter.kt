package com.stm.bledemo.filter
import kotlin.math.pow

class KalmanFilter(
    private var x: Double = 0.0, // Initial State
    private var F: Double = 1.0, // State Transition Matrix
    private var H: Double = 1.0, // Measurement Matrix
    private var P: Double = 1000.0, // Covariance Matrix
    private var R: Double = 15.0,  // Measurement Noise Covariance (Smaller value for faster response)
    private var Q: Double = 0.8  // Process Noise Covariance (Larger value for faster response)
) {

    fun predict() {
        x = F * x
        P = F * P * F + Q
    }

    fun update(z: Double) {
        val y = z - H * x
        val S = H * P * H + R
        val K = P * H / S
        x = x + K * y
        P = (1 - K * H) * P
    }

    fun filter(z: Int): Double {
        predict()
        update(z.toDouble())
        return x
    }
}

fun calculateDistance(rssi: Int, txPower: Double = -44.20289855, rssiN: Double = 4.830917874): Double {
    return 10.0.pow((txPower - rssi) / (10.0 * rssiN))
}



fun main() {
    val kalmanFilter = KalmanFilter()

    // Simulate streaming data (replace with your actual data source)
    val rssiStream = sequenceOf(-69, -70, -79, -69, -72, -65, -77, -74, -64, -71, -72, -69, -70, -70, -78, -76, -72, -77, -76, -73, -73, -71, -72, -79, -79, -74, -75, -76, -76, -81, -72, -77, -81, -77, -75, -83, -76, -83, -73, -79, -85, -83, -77, -79, -80, -85, -77, -84, -75, -81,-81, -78, -73, -76, -81, -77, -77, -76, -83, -85, -76, -77, -76, -75, -78, -75, -75, -79, -77, -78, -77, -77, -77, -78, -78, -76, -76, -79, -77, -78, -76, -76, -80, -77, -77, -77, -74, -73, -75, -75, -73, -73, -79, -74, -75, -78, -76, -76, -76, -78, -77, -77, -76, -75, -72, -73, -78, -78, -80, -76, -75, -75, -76, -71, -75, -85, -85, -75, -74, -74, -73, -79, -72, -71, -74, -73, -79, -71, -73, -79, -79, -71, -71, -72, -79, -79, -72, -74, -77, -78, -72, -73, -74, -78, -77, -72, -73, -69, -78, -69, -72, -72, -74, -78, -78, -72, -78, -74, -74, -75, -82, -76, -80, -72, -72, -80, -75, -77, -73, -75, -79, -79, -73, -76, -77, -79, -71, -76, -76, -78, -78, -72, -72, -75, -78, -78, -74, -79, -78, -74, -74, -78, -74, -74, -77, -73, -75, -71, -71, -72, -73, -75, -75, -71, -71, -72, -75, -68, -68, -69, -84, -83, -72, -73, -74, -78, -77, -76, -74, -73, -71, -76, -73, -73, -80, -82, -75, -73, -81, -75, -74, -81, -80, -76, -74, -77, -81, -80, -83, -80, -78, -76, -81, -76, -74, -79, -79, -75, -79, -75, -79, -75, -79, -76, -74, -73, -75, -69, -71, -79, -80, -81, -81, -82, -83, -82, -76, -77, -87, -80, -79, -86, -77, -79, -75, -76, -80, -76, -79, -80, -82, -79, -84, -83, -76, -87, -83, -82, -80, -80, -82, -82, -86, -75, -78, -77, -77, -81, -72, -76, -78, -79, -77, -76, -79, -80, -78, -78, -79, -79, -75, -85, -79, -79, -79, -79, -79, -76, -77, -70, -73, -71, -75, -74, -81, -74, -75, -76, -69, -77, -81, -85, -71, -79, -81, -90, -70, -72, -76, -79, -83, -73, -77, -70, -73, -72, -83, -70, -74, -75, -81, -84, -70, -75, -73, -80, -75, -81, -82, -69, -76, -82, -69, -76, -82, -82, -69, -84, -67, -63, -69, -71, -81, -71, -66, -71, -85, -73, -76, -75, -85, -87, -72, -77, -72, -84, -67, -66, -86, -71, -77, -76, -88, -72, -73, -89, -73, -73, -89, -79, -73, -83, -75, -83, -78, -73, -77, -73, -79, -83, -75, -81, -81, -75, -75, -80, -85, -75, -77, -82, -83, -84, -85, -75, -81, -83, -72, -72, -78, -78, -83, -83, -72, -79, -82, -72, -77, -78, -83, -81, -71, -71, -76, -76, -84, -85, -72, -83, -85, -71, -71, -78, -81, -82, -72, -72, -80, -78, -82, -72, -79, -78, -83, -83, -72, -72, -78, -82, -83, -72, -72, -81, -72, -77, -83, -72, -77, -84, -83, -71, -72, -87, -73, -73, -80, -79, -85, -74, -73, -80, -86, -88, -73, -80, -85, -84, -74, -74, -78, -84, -83, -73, -82, -84, -73, -73, -79, -79, -85, -85, -73, -79, -79, -73, -73, -80, -84, -73, -80, -81, -84, -74, -79, -80, -85, -74, -74, -81, -84, -79, -73, -85, -73, -80, -86, -73, -73, -80, -87, -87, -73, -73, -87, -73, -73, -80, -81, -84, -85, -84, -83, -73, -72, -80, -67, -72, -80, -81, -83, -73, -72, -78, -77, -81, -80, -72, -72, -78, -80, -74, -72, -82, -79, -76, -78, -73, -74, -82, -83, -76, -76, -80, -83, -83, -75, -71, -76, -75, -76, -75, -76, -75, -81, -77, -76, -76).asSequence()



    println("Filtered values and distances: ")
    rssiStream.forEach { rssi ->
        val filteredValue = kalmanFilter.filter(rssi)
        val distance = calculateDistance(rssi)
        val filteredDistance = calculateDistance(filteredValue.toInt())
        println("RSSI: $rssi, Filtered RSSI: $filteredValue, Distance: $distance, Filtered Distance: $filteredDistance")
    }

}
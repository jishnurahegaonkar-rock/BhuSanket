package com.bhusanket.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object BhuSanketApi {
    private const val baseUrl = "http://10.0.2.2:8001"
    private const val queueKey = "pending_reports"

    fun loadZones(): List<Zone>? = get("/api/zones")?.let { payload ->
        val rows = JSONArray(payload)
        (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val coordinates = row.optJSONArray("coordinates")
            Zone(
                id = row.getString("id"), name = row.getString("name"), district = row.getString("district"),
                score = row.optInt("risk_score", row.optInt("score", 0)),
                rain = row.optDouble("rainfall", 0.0), moisture = row.optInt("moisture", row.optInt("soil_moisture", 0)),
                temp = row.optDouble("temperature", 0.0), totalRain = row.optInt("accumulated", row.optInt("accumulated_rainfall", 0)),
                exposure = row.optInt("exposure", 0), latitude = coordinates?.optDouble(0) ?: 0.0, longitude = coordinates?.optDouble(1) ?: 0.0
            )
        }
    }

    fun loadNotifications(): String? = get("/api/notifications")

    fun queueReport(context: Context, report: JSONObject) {
        val preferences = context.getSharedPreferences("bhusanket", Context.MODE_PRIVATE)
        val queue = JSONArray(preferences.getString(queueKey, "[]"))
        queue.put(report)
        preferences.edit().putString(queueKey, queue.toString()).apply()
    }

    fun syncReports(context: Context): Int {
        val preferences = context.getSharedPreferences("bhusanket", Context.MODE_PRIVATE)
        val queued = JSONArray(preferences.getString(queueKey, "[]"))
        var synced = 0
        val remaining = JSONArray()
        for (index in 0 until queued.length()) {
            val report = queued.getJSONObject(index)
            if (post("/api/reports", report)) synced++ else remaining.put(report)
        }
        preferences.edit().putString(queueKey, remaining.toString()).apply()
        return synced
    }

    private fun get(path: String): String? = try {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 3500; connection.readTimeout = 5000; connection.requestMethod = "GET"
        if (connection.responseCode in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else null
    } catch (_: Exception) { null }

    private fun post(path: String, body: JSONObject): Boolean = try {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 3500; connection.readTimeout = 5000; connection.requestMethod = "POST"; connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        connection.responseCode in 200..299
    } catch (_: Exception) { false }
}

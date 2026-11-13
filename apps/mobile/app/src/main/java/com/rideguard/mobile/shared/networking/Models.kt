package com.rideguard.mobile.shared.networking

import kotlinx.serialization.Serializable

// Wire names deliberately match contracts/openapi.yaml, not the proposed contract.
@Serializable data class User(val id: String, val email: String, val full_name: String, val role: String, val status: String)
@Serializable data class Grant(val access_token: String, val refresh_token: String, val user: User)
@Serializable data class Page<T>(val items: List<T>, val next_cursor: String?)
@Serializable data class Session(val id: String, val client_type: String, val client_name: String?, val created_at: String, val last_seen_at: String, val expires_at: String, val is_current: Boolean)
@Serializable data class Device(val id: String, val binding_id: String, val serial_number: String, val nickname: String?, val model: String, val hardware_revision: String?, val status: String, val latest_report: Report?)
@Serializable data class Report(val observed_at: String, val received_at: String, val battery_percent: Int?, val radar_status: String, val imu_status: String)
@Serializable data class Binding(val id: String, val device_id: String, val serial_number: String, val nickname: String?, val bound_at: String, val unbound_at: String?, val status: String)
@Serializable data class Settings(val display_brightness: Int, val vibration_enabled: Boolean)
@Serializable data class Config(val binding_id: String, val schema_version: Int, val desired_version: Long, val reported_applied_version: Long?, val settings: Settings) {
    val pending: Boolean get() = reported_applied_version != desired_version
}
@Serializable data class Feedback(val id: String, val device_id: String?, val category: String, val content: String, val status: String, val admin_response: String?, val created_at: String, val responded_at: String?)
@Serializable data class ErrorEnvelope(val status: Int, val code: String, val message: String, val request_id: String? = null)

class ApiFailure(val status: Int, val code: String, val requestId: String? = null) : Exception(code)

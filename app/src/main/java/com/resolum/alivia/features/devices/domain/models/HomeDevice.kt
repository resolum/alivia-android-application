package com.resolum.alivia.features.devices.domain.models

enum class HomeDeviceType { HUB, MICROPHONE, ACTUATOR, SENSOR, OTHER }

/** A device the technician installed in the caregiver's home. */
data class HomeDevice(val id: Long, val alias: String, val macAddress: String, val type: HomeDeviceType)

/** The serviced home and its installed devices. */
data class HomeDevices(val address: String?, val contactName: String?, val devices: List<HomeDevice>)

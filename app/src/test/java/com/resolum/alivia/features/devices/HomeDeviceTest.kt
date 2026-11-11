package com.resolum.alivia.features.devices

import com.resolum.alivia.features.devices.data.datasources.remote.services.HomeDeviceService
import com.resolum.alivia.features.devices.data.datasources.remote.services.HomeDevicesResponseDto
import com.resolum.alivia.features.devices.data.repositories.HomeDeviceRepositoryImpl
import com.resolum.alivia.features.devices.domain.models.HomeDeviceType
import com.resolum.alivia.features.devices.domain.usecases.GetHomeDevicesUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDeviceTest {
    private class FakeService : HomeDeviceService {
        var failWith: Exception? = null
        override suspend fun mine(): HomeDevicesResponseDto {
            failWith?.let { throw it }
            return HomeDevicesResponseDto(
                " Av. Demo 101 ", " ",
                listOf(
                    HomeDevicesResponseDto.DeviceDto(1, " Hub Alivia ", "A4:C1:38:5E:02:01", "HUB"),
                    HomeDevicesResponseDto.DeviceDto(null, " ", "A4:C1:38:5E:02:02", "TOASTER"),
                    HomeDevicesResponseDto.DeviceDto(3, "Sin MAC", " ", "SENSOR"),
                ),
            )
        }
    }

    @Test
    fun mapsTheInstalledDevices() = runBlocking {
        val service = FakeService()
        val home = (GetHomeDevicesUseCase(HomeDeviceRepositoryImpl(service))() as NetworkResult.Success).data!!
        assertEquals("Av. Demo 101", home.address)
        assertNull(home.contactName)
        assertEquals(2, home.devices.size)
        assertEquals("Hub Alivia", home.devices[0].alias)
        assertEquals(HomeDeviceType.HUB, home.devices[0].type)
        assertEquals(2L, home.devices[1].id)
        assertEquals("Dispositivo", home.devices[1].alias)
        assertEquals(HomeDeviceType.OTHER, home.devices[1].type)
    }

    @Test
    fun treatsAHomeWithoutServiceAsEmpty() = runBlocking {
        val service = FakeService().apply { failWith = httpError(404) }
        val useCase = GetHomeDevicesUseCase(HomeDeviceRepositoryImpl(service))
        assertNull((useCase() as NetworkResult.Success).data)
        service.failWith = httpError(500)
        assertTrue(useCase() is NetworkResult.Error)
    }
}

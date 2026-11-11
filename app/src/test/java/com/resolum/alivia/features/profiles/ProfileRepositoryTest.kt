package com.resolum.alivia.features.profiles

import com.resolum.alivia.features.profiles.data.datasources.remote.ProfileFacadeService
import com.resolum.alivia.features.profiles.data.datasources.remote.models.HouseholdResponseDto
import com.resolum.alivia.features.profiles.data.datasources.remote.models.ProfileResponseDto
import com.resolum.alivia.features.profiles.data.datasources.remote.services.HouseholdService
import com.resolum.alivia.features.profiles.data.datasources.remote.services.ProfileService
import com.resolum.alivia.features.profiles.data.repositories.HouseholdRepositoryImpl
import com.resolum.alivia.features.profiles.data.repositories.ProfileRepositoryImpl
import com.resolum.alivia.features.profiles.domain.models.Household
import com.resolum.alivia.features.profiles.domain.models.Profile
import com.resolum.alivia.features.profiles.domain.models.ProfileChanges
import com.resolum.alivia.features.profiles.domain.usecases.GetMyHouseholdUseCase
import com.resolum.alivia.features.profiles.domain.usecases.GetProfileUseCase
import com.resolum.alivia.features.profiles.domain.usecases.UpdateProfileUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileRepositoryTest {
    private class FakeProfileService : ProfileService {
        var failWith: Exception? = null
        val sent = mutableListOf<String>()
        var photoSent = false
        var dto = ProfileResponseDto("ana@alivia.test", " https://img/ana.png ", "51900000001", "Ana", "Torres", "70000001", "1993-04-12")

        override suspend fun me(): ProfileResponseDto {
            failWith?.let { throw it }
            return dto
        }

        override suspend fun update(firstName: RequestBody?, lastName: RequestBody?, phoneNumber: RequestBody?, photo: MultipartBody.Part?): ProfileResponseDto {
            failWith?.let { throw it }
            listOf(firstName, lastName, phoneNumber).forEach { body ->
                sent += body?.let { Buffer().also(it::writeTo).readUtf8() } ?: "-"
            }
            photoSent = photo != null
            return dto.copy(firstName = sent[0].takeIf { it != "-" } ?: dto.firstName)
        }
    }

    private val current = Profile("Ana", "Torres", "ana@alivia.test", "51900000001", "70000001", LocalDate.of(1993, 4, 12), null)

    @Test
    fun mapsTheProfileAndItsNames() = runBlocking {
        val service = FakeProfileService()
        val profile = (GetProfileUseCase(ProfileRepositoryImpl(ProfileFacadeService(service)))() as NetworkResult.Success).data
        assertEquals("Ana Torres", profile.fullName)
        assertEquals("AT", profile.initials)
        assertEquals("https://img/ana.png", profile.photoUrl)
        assertEquals(LocalDate.of(1993, 4, 12), profile.birthDate)

        service.dto = ProfileResponseDto(phoneNumber = " ", birthDate = "not a date")
        val empty = (GetProfileUseCase(ProfileRepositoryImpl(ProfileFacadeService(service)))() as NetworkResult.Success).data
        assertNull(empty.phoneNumber)
        assertNull(empty.birthDate)
        assertEquals("", empty.fullName)
    }

    @Test
    fun explainsAMissingProfile() = runBlocking {
        val service = FakeProfileService().apply { failWith = httpError(404) }
        val result = GetProfileUseCase(ProfileRepositoryImpl(ProfileFacadeService(service)))() as NetworkResult.Error
        assertEquals("Tu cuenta todavía no tiene un perfil.", result.message)
        service.failWith = httpError(500)
        assertEquals(500, (GetProfileUseCase(ProfileRepositoryImpl(ProfileFacadeService(service)))() as NetworkResult.Error).code)
    }

    @Test
    fun sendsOnlyTheChangedFields() = runBlocking {
        val service = FakeProfileService()
        val update = UpdateProfileUseCase(ProfileRepositoryImpl(ProfileFacadeService(service)))
        assertEquals(NetworkResult.Success(current), update(current, " Ana ", "Torres", "+51 900000001"))
        assertTrue(service.sent.isEmpty())

        val changed = update(current, "Ana María", "Torres", "51911111111") as NetworkResult.Success
        assertEquals(listOf("Ana María", "-", "51911111111"), service.sent)
        assertEquals("Ana María", changed.data.firstName)

        assertTrue(update(ByteArray(3)) is NetworkResult.Success)
        assertTrue(service.photoSent)
    }

    @Test
    fun rejectsInvalidData() = runBlocking {
        val service = FakeProfileService()
        val update = UpdateProfileUseCase(ProfileRepositoryImpl(ProfileFacadeService(service)))
        assertEquals("Escribe tus nombres.", (update(current, " ", "Torres", "") as NetworkResult.Error).message)
        assertEquals("Escribe tus apellidos.", (update(current, "Ana", "", "") as NetworkResult.Error).message)
        assertEquals("El teléfono solo puede tener números.", (update(current, "Ana", "Torres", "9a9") as NetworkResult.Error).message)
        assertEquals("El teléfono no puede quedar vacío.", (update(current, "Ana", "Torres", "") as NetworkResult.Error).message)
        service.failWith = httpError(400)
        assertEquals(UpdateProfileUseCase.INVALID_DATA, (update(current, "Eva", "Torres", "51900000001") as NetworkResult.Error).message)
        service.failWith = httpError(503)
        assertEquals(503, (update(ByteArray(1)) as NetworkResult.Error).code)
        assertTrue(ProfileChanges().isEmpty)
        assertFalse(ProfileChanges(photo = ByteArray(1)).isEmpty)
    }

    private class FakeHouseholdService : HouseholdService {
        var failWith: Exception? = null
        var dto = HouseholdResponseDto(7, " Hogar demo 01 ", "Av. Demo 101", listOf(HouseholdResponseDto.MemberDto("PRINCIPAL", "ACTIVE")))
        override suspend fun mine(): HouseholdResponseDto {
            failWith?.let { throw it }
            return dto
        }
    }

    @Test
    fun readsTheHomeAndTheRoleInIt() = runBlocking {
        val service = FakeHouseholdService()
        val get = GetMyHouseholdUseCase(HouseholdRepositoryImpl(service))
        val home = (get() as NetworkResult.Success).data!!
        assertEquals(Household(7, "Hogar demo 01", "Av. Demo 101", "PRINCIPAL"), home)
        assertTrue(home.canPlanActivities)

        service.dto = HouseholdResponseDto(8, "", null, listOf(HouseholdResponseDto.MemberDto("CAREGIVER", null)))
        val caregiver = (get() as NetworkResult.Success).data!!
        assertEquals("Mi hogar", caregiver.alias)
        assertFalse(caregiver.canPlanActivities)

        service.dto = HouseholdResponseDto()
        assertNull((get() as NetworkResult.Success).data)
        service.failWith = httpError(404)
        assertNull((get() as NetworkResult.Success).data)
        service.failWith = httpError(500)
        assertTrue(get() is NetworkResult.Error)
    }
}

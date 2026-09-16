package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddComponentFormTest {

    @Test
    fun submit_blankPriorDistance_usesUnknownZeroBaseline() {
        val submission = validState(priorDistanceText = "").submit()

        assertEquals(0.0, submission.request().baselineKm, 0.0)
        assertEquals(PriorUsageCertainty.UNKNOWN, submission.request().priorUsageCertainty)
    }

    @Test
    fun submit_explicitZero_usesKnownZeroBaseline() {
        val submission = validState(priorDistanceText = "0").submit()

        assertEquals(0.0, submission.request().baselineKm, 0.0)
        assertEquals(PriorUsageCertainty.KNOWN, submission.request().priorUsageCertainty)
    }

    @Test
    fun submit_positivePriorDistance_usesKnownBaseline() {
        val submission = validState(priorDistanceText = "1200").submit()

        assertEquals(1200.0, submission.request().baselineKm, 0.0)
        assertEquals(PriorUsageCertainty.KNOWN, submission.request().priorUsageCertainty)
    }

    @Test
    fun submit_approximatePriorDistance_usesApproximateBaseline() {
        val submission = validState(priorDistanceText = "1200", approximate = true).submit()

        assertEquals(1200.0, submission.request().baselineKm, 0.0)
        assertEquals(PriorUsageCertainty.APPROXIMATE, submission.request().priorUsageCertainty)
    }

    @Test
    fun submit_blankPriorDistance_ignoresApproximateSelection() {
        val submission = validState(priorDistanceText = "  ", approximate = true).submit()

        assertEquals(0.0, submission.request().baselineKm, 0.0)
        assertEquals(PriorUsageCertainty.UNKNOWN, submission.request().priorUsageCertainty)
    }

    @Test
    fun submit_negativePriorDistance_isRejected() {
        assertEquals(
            AddComponentFormSubmission.InvalidPriorDistance,
            validState(priorDistanceText = "-1").submit(),
        )
    }

    @Test
    fun submit_nonNumericPriorDistance_isRejected() {
        assertEquals(
            AddComponentFormSubmission.InvalidPriorDistance,
            validState(priorDistanceText = "many").submit(),
        )
    }

    @Test
    fun submit_nonFinitePriorDistance_isRejected() {
        assertEquals(
            AddComponentFormSubmission.InvalidPriorDistance,
            validState(priorDistanceText = "NaN").submit(),
        )
    }

    @Test
    fun submit_blankOptionalIdentificationFields_isValid() {
        val submission = validState(displayName = "", make = "", model = "").submit()

        assertEquals("", submission.request().displayName)
        assertEquals("", submission.request().make)
        assertEquals("", submission.request().model)
    }

    @Test
    fun submit_identificationFields_areTrimmed() {
        val submission = validState(
            displayName = " Race chain ",
            make = " Shimano ",
            model = " CN-HG54 ",
        ).submit()

        assertEquals("Race chain", submission.request().displayName)
        assertEquals("Shimano", submission.request().make)
        assertEquals("CN-HG54", submission.request().model)
    }

    @Test
    fun submit_selectedType_usesExistingTypeKeyAndDefaultLifespan() {
        val submission = validState(typeKey = "cassette").submit()

        assertEquals("cassette", submission.request().type)
        assertEquals(10_000.0, submission.request().lifespanKm, 0.0)
    }

    @Test
    fun toEntity_bikeDestination_isInstalledOnBike() {
        val entity = validState().submit().request().toEntity(bikeId = 42L, installedAt = 100L)

        assertEquals(42L, entity.bikeId)
        assertEquals(ComponentLifecycleStatus.INSTALLED, entity.lifecycleStatus)
    }

    @Test
    fun toEntity_garageDestination_isUnassignedAndInGarage() {
        val entity = validState().submit().request().toEntity(bikeId = null, installedAt = 100L)

        assertEquals(null, entity.bikeId)
        assertEquals(ComponentLifecycleStatus.IN_GARAGE, entity.lifecycleStatus)
    }

    private fun validState(
        typeKey: String = "chain",
        displayName: String = "",
        make: String = "",
        model: String = "",
        priorDistanceText: String = "",
        approximate: Boolean = false,
    ) = AddComponentFormState(
        typeKey = typeKey,
        displayName = displayName,
        make = make,
        model = model,
        priorDistanceText = priorDistanceText,
        approximate = approximate,
    )

    private fun AddComponentFormSubmission.request(): AddComponentRequest {
        assertTrue(this is AddComponentFormSubmission.Valid)
        return (this as AddComponentFormSubmission.Valid).request
    }
}

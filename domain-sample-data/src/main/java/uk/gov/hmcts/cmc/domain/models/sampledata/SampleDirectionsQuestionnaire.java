package uk.gov.hmcts.cmc.domain.models.sampledata;

import uk.gov.hmcts.cmc.domain.models.directionsquestionnaire.DirectionsQuestionnaire;
import uk.gov.hmcts.cmc.domain.models.directionsquestionnaire.ExpertReport;
import uk.gov.hmcts.cmc.domain.models.directionsquestionnaire.UnavailableDate;
import uk.gov.hmcts.cmc.domain.models.response.YesNoOption;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;

public class SampleDirectionsQuestionnaire {

    public static SampleDirectionsQuestionnaire builder() {
        return new SampleDirectionsQuestionnaire();
    }

    public DirectionsQuestionnaire build() {
        List<UnavailableDate> unavailableDates = asList(
            new UnavailableDate("1", LocalDate.of(2050, 1, 1)));

        List<ExpertReport> expertReportRowsData = asList(
            new ExpertReport("1", "expert1", LocalDate.of(2040, 1, 1)));

        return DirectionsQuestionnaire.builder()
            .selfWitness(YesNoOption.YES)
            .howManyOtherWitness(1)
            .hearingLocation("London")
            .hearingLocationSlug("London-Court")
            .exceptionalCircumstancesReason("disabled")
            .unavailableDates(unavailableDates)
            .availableDate(LocalDate.of(2050, 1, 2))
            .languageInterpreted("some language")
            .signLanguageInterpreted("some sign language")
            .hearingLoop(YesNoOption.NO)
            .disabledAccess(YesNoOption.YES)
            .otherSupportRequired("maybe")
            .expertReportsRows(expertReportRowsData)
            .expertEvidenceToExamine("nothing")
            .reasonForExpertAdvice("for specified reason")
            .build();
    }
}
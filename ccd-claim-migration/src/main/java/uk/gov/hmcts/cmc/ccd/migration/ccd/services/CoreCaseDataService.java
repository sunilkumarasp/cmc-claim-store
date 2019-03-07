package uk.gov.hmcts.cmc.ccd.migration.ccd.services;

import com.google.common.collect.ImmutableMap;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cmc.ccd.domain.CaseEvent;
import uk.gov.hmcts.cmc.ccd.migration.ccd.services.exceptions.CreateCaseException;
import uk.gov.hmcts.cmc.ccd.migration.ccd.services.exceptions.OverwriteCaseException;
import uk.gov.hmcts.cmc.ccd.migration.idam.models.User;
import uk.gov.hmcts.cmc.ccd.migration.stereotypes.LogExecutionTime;
import uk.gov.hmcts.cmc.domain.models.Claim;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "core_case_data", name = "api.url")
public class CoreCaseDataService {

    enum EventType {
        MIGRATED_FROM_CLAIMSTORE_CREATE("MigrationFromClaimstoreCreate"),
        MIGRATED_FROM_CLAIMSTORE_UPDATE("MigrationFromClaimstoreUpdate");

        private String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    public static final String JURISDICTION_ID = "CMC";
    public static final String CASE_TYPE_ID = "MoneyClaimCase";

    private static final Logger logger = LoggerFactory.getLogger(CoreCaseDataService.class);

    private final CoreCaseDataApi coreCaseDataApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final MigrateCoreCaseDataService migrateCoreCaseDataService;

    @Autowired
    public CoreCaseDataService(
        MigrateCoreCaseDataService migrateCoreCaseDataService,
        CoreCaseDataApi coreCaseDataApi,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.migrateCoreCaseDataService = migrateCoreCaseDataService;
        this.coreCaseDataApi = coreCaseDataApi;
        this.authTokenGenerator = authTokenGenerator;
    }

    @LogExecutionTime
    public CaseDetails createCase(User user, Claim claim, CaseEvent event) {
        try {

            EventRequestData eventRequestData = EventRequestData.builder()
                .userId(user.getUserDetails().getId())
                .jurisdictionId(JURISDICTION_ID)
                .caseTypeId(CASE_TYPE_ID)
                .eventId(event.getValue())
                .ignoreWarning(true)
                .build();

            return migrateCoreCaseDataService.save(user.getAuthorisation(), eventRequestData, claim);
        } catch (Exception exception) {
            throw new CreateCaseException(
                String.format("Failed storing claim in CCD store for claim on %s on event %s",
                    claim.getReferenceNumber(), event.getValue()),
                exception
            );
        }
    }

    @LogExecutionTime
    public void updateCase(User user, Long caseId, Claim claim, CaseEvent event) {

        try {
            EventRequestData eventRequestData = EventRequestData.builder()
                .userId(user.getUserDetails().getId())
                .jurisdictionId(JURISDICTION_ID)
                .caseTypeId(CASE_TYPE_ID)
                .eventId(event.getValue())
                .ignoreWarning(true)
                .build();

            migrateCoreCaseDataService
                .update(user.getAuthorisation(), eventRequestData, caseId, claim);
        } catch (Exception exception) {
            throw new OverwriteCaseException(
                String.format(
                    "Failed updating claim in CCD store for claim %s on event %s",
                    claim.getReferenceNumber(),
                    event), exception
            );
        }
    }

    @Retryable(value = {SocketTimeoutException.class, FeignException.class, IOException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 400, maxDelay = 800)
    )
    @LogExecutionTime
    public Optional<CaseDetails> getCcdIdByReferenceNumber(User user, String referenceNumber) {
        return search(user, ImmutableMap.of("case.referenceNumber", referenceNumber));
    }

    @Recover
    public void recoverSearchFailure(SocketTimeoutException exception, User user, String referenceNumber) {
        String errorMessage = String.format(
            "Failure: failed search by reference number ( %s for user %s ) due to %s",
            referenceNumber, user.getUserDetails().getId(), exception.getMessage()
        );

        logger.info(errorMessage, exception);
    }

    private Optional<CaseDetails> search(User user, Map<String, String> searchString) {

        List<CaseDetails> result;
        result = this.coreCaseDataApi.searchForCaseworker(
            user.getAuthorisation(),
            this.authTokenGenerator.generate(),
            user.getUserDetails().getId(),
            JURISDICTION_ID,
            CASE_TYPE_ID,
            searchString
        );

        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
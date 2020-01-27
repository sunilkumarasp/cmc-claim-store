package uk.gov.hmcts.cmc.claimstore.services.ccd.callbacks.response;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cmc.ccd.domain.CCDYesNoOption;
import uk.gov.hmcts.cmc.ccd.domain.CaseEvent;
import uk.gov.hmcts.cmc.ccd.mapper.CaseMapper;
import uk.gov.hmcts.cmc.claimstore.config.properties.notifications.NotificationsProperties;
import uk.gov.hmcts.cmc.claimstore.rules.MoreTimeRequestRule;
import uk.gov.hmcts.cmc.claimstore.services.ResponseDeadlineCalculator;
import uk.gov.hmcts.cmc.claimstore.services.ccd.Role;
import uk.gov.hmcts.cmc.claimstore.services.ccd.callbacks.Callback;
import uk.gov.hmcts.cmc.claimstore.services.ccd.callbacks.CallbackHandler;
import uk.gov.hmcts.cmc.claimstore.services.ccd.callbacks.CallbackParams;
import uk.gov.hmcts.cmc.claimstore.services.ccd.callbacks.CallbackType;
import uk.gov.hmcts.cmc.claimstore.services.notifications.NotificationReferenceBuilder;
import uk.gov.hmcts.cmc.claimstore.services.notifications.NotificationService;
import uk.gov.hmcts.cmc.claimstore.utils.CaseDetailsConverter;
import uk.gov.hmcts.cmc.domain.models.Claim;
import uk.gov.hmcts.cmc.domain.models.ClaimDocument;
import uk.gov.hmcts.cmc.domain.models.ClaimDocumentCollection;
import uk.gov.hmcts.cmc.domain.models.ClaimDocumentType;
import uk.gov.hmcts.cmc.domain.models.ScannedDocument;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse.AboutToStartOrSubmitCallbackResponseBuilder;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static uk.gov.hmcts.cmc.claimstore.services.ccd.Role.CASEWORKER;
import static uk.gov.hmcts.cmc.claimstore.services.notifications.content.NotificationTemplateParameters.CLAIMANT_NAME;
import static uk.gov.hmcts.cmc.claimstore.services.notifications.content.NotificationTemplateParameters.CLAIM_REFERENCE_NUMBER;
import static uk.gov.hmcts.cmc.claimstore.services.notifications.content.NotificationTemplateParameters.DEFENDANT_NAME;
import static uk.gov.hmcts.cmc.claimstore.services.notifications.content.NotificationTemplateParameters.FRONTEND_BASE_URL;

@Service
public class PaperResponseReviewedCallbackHandler extends CallbackHandler {

    private static final List<Role> ROLES = Collections.singletonList(CASEWORKER);

    private final CaseDetailsConverter caseDetailsConverter;
    private final CaseMapper caseMapper;

    private static List<ClaimDocumentType> paperResponseStaffUploadedType = Arrays.asList(
        ClaimDocumentType.PAPER_RESPONSE_FULL_ADMIT,
        ClaimDocumentType.PAPER_RESPONSE_DISPUTES_ALL,
        ClaimDocumentType.PAPER_RESPONSE_PART_ADMIT,
        ClaimDocumentType.PAPER_RESPONSE_STATES_PAID);

    private static List<String> paperResponseScannedType = Arrays.asList("N9a",
        "N9b",
        "N11");

    private static Function<Claim, List<ClaimDocument>> getStaffUploadedDocs = claim ->
        claim.getClaimDocumentCollection()
            .map(ClaimDocumentCollection::getStaffUploadedDocuments)
            .orElse(Collections.emptyList());

    private static Function<Claim, List<ScannedDocument>> getBulkScannedDocs = claim ->
        claim.getClaimDocumentCollection()
            .map(ClaimDocumentCollection::getScannedDocuments)
            .orElse(Collections.emptyList());

    private static Predicate<ClaimDocument> filterClaimDocumentPaperResponseDoc = doc ->
        paperResponseStaffUploadedType.stream()
            .anyMatch(type -> type.equals(doc.getDocumentType()));

    private static Predicate<ScannedDocument> filterScannedDocumentPaperResponseDoc = doc ->
        paperResponseScannedType.stream().anyMatch(type -> type.equalsIgnoreCase(doc.getSubtype()));

    private static Predicate<ClaimDocument> isClaimDocumentMoreTimeRequested = doc ->
        doc.getDocumentType().equals(ClaimDocumentType.PAPER_RESPONSE_MORE_TIME);

    private static Predicate<ScannedDocument> isScannedDocumentMoreTimeRequested = doc ->
        doc.getSubtype().equalsIgnoreCase("N9");

    private final ResponseDeadlineCalculator responseDeadlineCalculator;
    private final MoreTimeRequestRule moreTimeRequestRule;
    private final NotificationService notificationService;
    private final NotificationsProperties notificationsProperties;

    @Autowired
    public PaperResponseReviewedCallbackHandler(
        CaseDetailsConverter caseDetailsConverter,
        CaseMapper caseMapper,
        ResponseDeadlineCalculator responseDeadlineCalculator,
        MoreTimeRequestRule moreTimeRequestRule,
        NotificationService notificationService,
        NotificationsProperties notificationsProperties
    ) {
        this.caseDetailsConverter = caseDetailsConverter;
        this.caseMapper = caseMapper;
        this.responseDeadlineCalculator = responseDeadlineCalculator;
        this.moreTimeRequestRule = moreTimeRequestRule;
        this.notificationService = notificationService;
        this.notificationsProperties = notificationsProperties;
    }

    @Override
    protected Map<CallbackType, Callback> callbacks() {
        return ImmutableMap.of(
            CallbackType.ABOUT_TO_START, this::verifyResponsePossible,
            CallbackType.ABOUT_TO_SUBMIT, this::updateResponseDeadline
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return Collections.singletonList(CaseEvent.REVIEWED_PAPER_RESPONSE);
    }

    @Override
    public List<Role> getSupportedRoles() {
        return ROLES;
    }

    private AboutToStartOrSubmitCallbackResponse updateResponseDeadline(CallbackParams callbackParams) {
        CallbackRequest callbackRequest = callbackParams.getRequest();
        Claim claim = toClaimAfterEvent(callbackRequest);

        if (verifyNoDuplicateMoreTimeRequested(callbackRequest)) {
            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(Collections.singletonList("Requesting More Time to respond can be done only once."))
                .build();
        }

        Optional<AboutToStartOrSubmitCallbackResponse> isPaperResponseMoreTimeRequested =
            updateMoreTimeRequestedResponse(callbackRequest);

        if (isPaperResponseMoreTimeRequested.isPresent()) {
            return isPaperResponseMoreTimeRequested.orElseThrow(IllegalArgumentException::new);
        }

        try {
            LocalDateTime responseTime = getReceivedTimeForStaffUploadedPaperResponseDoc(claim)
                .map(ClaimDocument::getReceivedDateTime)
                .orElseGet(() -> getScanedTimeForScannedPaperResponseDoc(claim)
                    .map(ScannedDocument::getDeliveryDate)
                    .orElseThrow(IllegalArgumentException::new)
                );

            Map<String, Object> data = caseDetailsConverter.convertToMap(
                caseMapper.to(
                    claim.toBuilder().respondedAt(responseTime).build()
                ));

            notifyClaimant(claim);

            return AboutToStartOrSubmitCallbackResponse.builder()
                .data(data)
                .build();

        } catch (Exception gene) {
            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(Collections.singletonList("Unable to determine the response time."))
                .build();
        }
    }

    private AboutToStartOrSubmitCallbackResponse verifyResponsePossible(
        CallbackParams callbackParams) {
        Claim claim = toClaimAfterEvent(callbackParams.getRequest());
        List<String> validationErrors = new ArrayList<>();

        if (claim.getResponse().isPresent() || claim.getRespondedAt() != null) {
            validationErrors.add(MoreTimeRequestRule.ALREADY_RESPONDED_ERROR);
        }

        return AboutToStartOrSubmitCallbackResponse
            .builder()
            .errors(validationErrors)
            .build();
    }

    private Claim toClaimAfterEvent(CallbackRequest callbackRequest) {
        return caseDetailsConverter.extractClaim(callbackRequest.getCaseDetails());
    }

    private Claim toClaimBeforeEvent(CallbackRequest callbackRequest) {
        return caseDetailsConverter.extractClaim(callbackRequest.getCaseDetailsBefore());
    }

    private List<ClaimDocument> getMoreTimeRequestedStaffUploadedDocs(Claim claim) {
        return getStaffUploadedDocs.apply(claim)
            .stream()
            .filter(isClaimDocumentMoreTimeRequested)
            .collect(Collectors.toList());
    }

    private List<ScannedDocument> getMoreTimeRequestedBulkScanDocs(Claim claim) {
        return getBulkScannedDocs.apply(claim)
            .stream()
            .filter(isScannedDocumentMoreTimeRequested)
            .collect(Collectors.toList());
    }

    private List<ClaimDocument> getStaffUploadedMoreTimeRequestedForEvent(CallbackRequest callbackRequest) {
        return getStaffUploadedDocs.apply(toClaimAfterEvent(callbackRequest))
            .stream().filter(doc -> !getStaffUploadedDocs.apply(toClaimBeforeEvent(callbackRequest)).contains(doc))
            .filter(isClaimDocumentMoreTimeRequested)
            .collect(Collectors.toList());
    }

    private List<ScannedDocument> getBulkScannedDocMoreTimeRequestedForEvent(CallbackRequest callbackRequest) {
        return getBulkScannedDocs.apply(toClaimAfterEvent(callbackRequest))
            .stream().filter(doc -> !getBulkScannedDocs.apply(toClaimBeforeEvent(callbackRequest)).contains(doc))
            .filter(isScannedDocumentMoreTimeRequested)
            .collect(Collectors.toList());
    }

    private Optional<ClaimDocument> getReceivedTimeForStaffUploadedPaperResponseDoc(Claim claim) {
        return getStaffUploadedDocs.apply(claim)
            .stream()
            .filter(filterClaimDocumentPaperResponseDoc)
            .findFirst();
    }

    private Optional<ScannedDocument> getScanedTimeForScannedPaperResponseDoc(Claim claim) {
        return getBulkScannedDocs.apply(claim)
            .stream()
            .filter(filterScannedDocumentPaperResponseDoc)
            .findFirst();
    }

    private boolean verifyNoDuplicateMoreTimeRequested(CallbackRequest request) {

        return getMoreTimeRequestedStaffUploadedDocs(toClaimAfterEvent(request)).size()
            + getMoreTimeRequestedBulkScanDocs(toClaimAfterEvent(request)).size() > 1;
    }

    private Optional<AboutToStartOrSubmitCallbackResponse> updateMoreTimeRequestedResponse(CallbackRequest request) {
        Claim claimByEvent = toClaimAfterEvent(request);

        if (getStaffUploadedMoreTimeRequestedForEvent(request).isEmpty()
            && getBulkScannedDocMoreTimeRequestedForEvent(request).isEmpty()) {
            return Optional.empty();
        }

        LocalDate newDeadline =
            responseDeadlineCalculator.calculatePostponedResponseDeadline(claimByEvent.getIssuedOn());

        List<String> validationResult = moreTimeRequestRule.validateMoreTimeCanBeRequested(claimByEvent, newDeadline);
        AboutToStartOrSubmitCallbackResponseBuilder builder = AboutToStartOrSubmitCallbackResponse.builder();

        if (!validationResult.isEmpty()) {
            return Optional.of(AboutToStartOrSubmitCallbackResponse
                .builder()
                .errors(validationResult)
                .build());
        }

        Map<String, Object> data = caseDetailsConverter.convertToMap(caseMapper.to(claimByEvent));
        data.put("moreTimeRequested", CCDYesNoOption.YES);
        data.put("responseDeadline", newDeadline);

        return Optional.of(builder
            .data(data)
            .build());
    }

    private void notifyClaimant(Claim claim) {
        notificationService.sendMail(
            claim.getSubmitterEmail(),
            notificationsProperties.getTemplates().getEmail().getClaimantPaperResponseReceived(),
            aggregateParams(claim),
            NotificationReferenceBuilder.PaperResponse
                .notifyClaimantPaperResponseSubmitted(claim.getReferenceNumber(), "claimant")
        );
    }

    private Map<String, String> aggregateParams(Claim claim) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(CLAIMANT_NAME, claim.getClaimData().getClaimant().getName());
        parameters.put(DEFENDANT_NAME, claim.getClaimData().getDefendant().getName());
        parameters.put(FRONTEND_BASE_URL, notificationsProperties.getFrontendBaseUrl());
        parameters.put(CLAIM_REFERENCE_NUMBER, claim.getReferenceNumber());
        return parameters;
    }
}

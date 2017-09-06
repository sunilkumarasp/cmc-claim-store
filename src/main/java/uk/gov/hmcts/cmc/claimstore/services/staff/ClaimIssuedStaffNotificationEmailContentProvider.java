package uk.gov.hmcts.cmc.claimstore.services.staff;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cmc.claimstore.models.Claim;
import uk.gov.hmcts.cmc.claimstore.services.staff.models.EmailContent;
import uk.gov.hmcts.cmc.claimstore.stereotypes.EmailContentProvider;

import static java.lang.String.format;
import static uk.gov.hmcts.cmc.claimstore.utils.Preconditions.requireNonBlank;

@Component
public class ClaimIssuedStaffNotificationEmailContentProvider implements EmailContentProvider<Claim> {

    @Override
    public EmailContent createContent(Claim claim) {
        requireNonBlank(claim.getReferenceNumber());

        return new EmailContent(
            claim.getClaimData().isClaimantRepresented()
                ? format("Claim form %s", claim.getReferenceNumber())
                : format("Claim %s issued", claim.getReferenceNumber()),
            "Please find attached claim."
        );
    }

}

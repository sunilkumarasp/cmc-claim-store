package uk.gov.hmcts.cmc.claimstore.services.staff.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cmc.claimstore.config.properties.emails.StaffEmailTemplates;
import uk.gov.hmcts.cmc.claimstore.services.TemplateService;
import uk.gov.hmcts.cmc.claimstore.services.staff.models.EmailContent;
import uk.gov.hmcts.cmc.claimstore.stereotypes.EmailContentProvider;

import java.util.Map;

@Service
public class FullDefenceStaffEmailContentProvider
    implements EmailContentProvider<Map<String, Object>> {

    private final TemplateService templateService;
    private final StaffEmailTemplates emailTemplates;

    @Autowired
    public FullDefenceStaffEmailContentProvider(
        TemplateService templateService,
        StaffEmailTemplates emailTemplates
    ) {
        this.templateService = templateService;
        this.emailTemplates = emailTemplates;
    }

    @Override
    public EmailContent createContent(Map<String, Object> input) {
        return new EmailContent(
            evaluateTemplate(emailTemplates.getFullDefenceResponseEmailSubject(), input),
            evaluateTemplate(emailTemplates.getFullDefenceResponseEmailBody(), input)
        );
    }

    @Override
    public TemplateService getTemplateService() {
        return templateService;
    }
}

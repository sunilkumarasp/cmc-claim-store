package uk.gov.hmcts.cmc.domain.models.party;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.ccd.definition.FieldLabel;
import uk.gov.hmcts.cmc.domain.models.Address;
import uk.gov.hmcts.cmc.domain.models.legalrep.Representative;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class Organisation extends Party implements HasContactPerson {

    @FieldLabel("Contact Person")
    private final String contactPerson;
    @FieldLabel("Companies house number")
    private final String companiesHouseNumber;

    @Builder
    public Organisation(
        String name,
        Address address,
        Address correspondenceAddress,
        String mobilePhone,
        Representative representative,
        String contactPerson,
        String companiesHouseNumber
    ) {
        super(name, address, correspondenceAddress, mobilePhone, representative);
        this.contactPerson = contactPerson;
        this.companiesHouseNumber = companiesHouseNumber;
    }

    public Optional<String> getContactPerson() {
        return Optional.ofNullable(contactPerson);
    }

    public Optional<String> getCompaniesHouseNumber() {
        return Optional.ofNullable(companiesHouseNumber);
    }

}

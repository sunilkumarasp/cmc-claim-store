package uk.gov.hmcts.cmc.claimstore.models.party;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.hmcts.cmc.claimstore.models.Address;
import uk.gov.hmcts.cmc.claimstore.models.legalrep.Representative;

import java.util.Objects;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Organisation extends Party {

    private final String contactPerson;
    private final String companiesHouseNumber;

    public Organisation(
        String name,
        Address address,
        Address correspondenceAddress,
        String mobilePhone,
        Representative representative,
        String contactPerson,
        String companiesHouseNumber) {
        super(name, address, correspondenceAddress, mobilePhone, representative);
        this.contactPerson = contactPerson;
        this.companiesHouseNumber = companiesHouseNumber;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public Optional<String> getCompaniesHouseNumber() {
        return Optional.ofNullable(companiesHouseNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Organisation other = (Organisation) obj;

        return super.equals(other) && Objects.equals(contactPerson, other.contactPerson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contactPerson);
    }

}

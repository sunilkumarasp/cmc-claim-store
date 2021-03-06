package uk.gov.hmcts.cmc.claimstore.courtfinder.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class Court {
    private String name;
    private String slug;
    private Address address;
}

package uk.gov.hmcts.cmc.claimstore.controllers;

import org.junit.Test;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.cmc.claimstore.BaseTest;
import uk.gov.hmcts.cmc.claimstore.models.Claim;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cmc.claimstore.controllers.utils.sampledata.SampleClaim.SUBMITTER_EMAIL;

public class LinkDefendantToClaimTest extends BaseTest {

    @Test
    public void shouldReturn200HttpStatusAndUpdatedClaimWhenLinkIsSuccessfullySet() throws Exception {
        Long claimId = 1L;
        Long defendantId = 2L;

        given(claimRepository.getById(claimId))
            .willReturn(Optional.of(newClaim(claimId, null)))
            .willReturn(Optional.of(newClaim(claimId, defendantId)));

        MvcResult result = webClient
            .perform(put("/claims/" + claimId + "/defendant/" + defendantId))
            .andExpect(status().isOk())
            .andReturn();

        Claim responseBody = jsonMapper.fromJson(result.getResponse().getContentAsString(), Claim.class);
        assertThat(responseBody.getDefendantId()).isEqualTo(defendantId);
    }

    @Test
    public void shouldReturn404HttpStatusWhenClaimDoesNotExist() throws Exception {
        Long claimId = 1L;

        given(claimRepository.getById(claimId)).willReturn(Optional.empty());

        webClient
            .perform(put("/claims/" + claimId + "/defendant/2"))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void shouldReturn404HttpStatusWhenClaimParameterIsNotNumber() throws Exception {
        webClient
            .perform(put("/claims/not-a-number/defendant/2"))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void shouldReturn404HttpStatusWhenDefendantParameterIsNotNumber() throws Exception {
        webClient
            .perform(put("/claims/1/defendant/not-a-number"))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void shouldReturn500HttpStatusWhenFailedToRetrieveClaim() throws Exception {
        Long claimId = 1L;

        given(claimRepository.getById(claimId))
            .willThrow(new UnableToExecuteStatementException("Unexpected error", (StatementContext) null));

        webClient
            .perform(put("/claims/" + claimId + "/defendant/2"))
            .andExpect(status().isInternalServerError())
            .andReturn();
    }

    @Test
    public void shouldReturn500HttpStatusWhenFailedToMakeLink() throws Exception {
        Long claimId = 1L;
        Long defendantId = 2L;

        given(claimRepository.getById(claimId)).willReturn(Optional.of(newClaim(claimId, null)));
        given(claimRepository.linkDefendant(claimId, defendantId))
            .willThrow(new UnableToExecuteStatementException("Unexpected error", (StatementContext) null));

        webClient
            .perform(put("/claims/" + claimId + "/defendant/" + defendantId))
            .andExpect(status().isInternalServerError())
            .andReturn();
    }

    private Claim newClaim(Long id, Long defendantId) {
        return new Claim(
            id,
            SUBMITTER_ID,
            LETTER_HOLDER_ID,
            defendantId,
            null,
            REFERENCE_NUMBER,
            null,
            null,
            null,
            null,
            false,
            SUBMITTER_EMAIL, null);
    }
}

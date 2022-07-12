package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.reference.data.model.JudicialMemberType.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.reference.data.model.JudicialMemberType;

@RunWith(MockitoJUnitRunner.class)
public class UpdateListingRequirementsUtilTest {

    @InjectMocks
    private UpdateListingRequirementsUtil utils;

    @Test
    public void isValidJudicialMemberTypePresident() {
        boolean result = utils.isValidJudicialMemberType("President of Tribunal");

        assertThat(result).isTrue();
    }

    @Test
    public void isValidJudicialMemberTypeRegional() {
        boolean result = utils.isValidJudicialMemberType("Regional Tribunal Judge");

        assertThat(result).isTrue();
    }


    @Test
    public void isValidJudicialMemberTypeJudge() {
        boolean result = utils.isValidJudicialMemberType("Tribunal Judge");

        assertThat(result).isTrue();
    }


    @Test
    public void isValidJudicialMemberType() {
        boolean result = utils.isValidJudicialMemberType("Wrong Type");

        assertThat(result).isFalse();
    }

    @Test
    public void getJudicialMemberTypePresident() {
        JudicialMemberType result = utils.getJudicialMemberType("President of Tribunal");

        assertThat(result).isEqualTo(TRIBUNAL_PRESIDENT);
    }

    @Test
    public void getJudicialMemberTypeRegional() {
        JudicialMemberType result = utils.getJudicialMemberType("Regional Tribunal Judge");

        assertThat(result).isEqualTo(REGIONAL_TRIBUNAL_JUDGE);
    }

    @Test
    public void getJudicialMemberTypeJudge() {
        JudicialMemberType result = utils.getJudicialMemberType("Tribunal Judge");

        assertThat(result).isEqualTo(TRIBUNAL_JUDGE);
    }

    @Test
    public void getJudicialMemberType() {
        JudicialMemberType result = utils.getJudicialMemberType("Test");

        assertThat(result).isNull();
    }

}

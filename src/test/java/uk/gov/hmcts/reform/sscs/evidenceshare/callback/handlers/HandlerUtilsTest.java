package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerUtils.isANewJointParty;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@RunWith(JUnitParamsRunner.class)
public class HandlerUtilsTest {

    @Test
    public void givenAJointPartyIsNew_thenReturnTrue() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
            .appeal(Appeal.builder().build())
            .build();

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData,
            WITH_DWP, DWP_UPLOAD_RESPONSE);

        assertTrue(isANewJointParty(callback, caseData));
    }

    @Test
    public void givenAJointPartyIsExisting_thenReturnFalse() {
        SscsCaseData oldCaseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
            .appeal(Appeal.builder().build())
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
            .appeal(Appeal.builder().build())
            .build();

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, oldCaseData,
            WITH_DWP, DWP_UPLOAD_RESPONSE);

        assertFalse(isANewJointParty(callback, caseData));
    }

    @Test
    public void givenAJointPartyWasNoIsYes_thenReturnTrue() {
        SscsCaseData oldCaseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(NO).build())
            .appeal(Appeal.builder().build())
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
            .appeal(Appeal.builder().build())
            .build();

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, oldCaseData,
            WITH_DWP, DWP_UPLOAD_RESPONSE);

        assertTrue(isANewJointParty(callback, caseData));
    }
}

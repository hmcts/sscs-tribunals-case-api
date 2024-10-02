import {Page} from '@playwright/test';
import {BaseStep} from './base';
import {credentials} from "../../config/config";
import writeFinalDecisionData from "../../pages/content/write.final.decision_en.json";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import {accessId, accessToken, getSSCSServiceToken} from "../../api/client/idam/idam.service";
import {
    performEventOnCaseForActionFurtherEvidence,
    performEventOnCaseWithUploadResponse
} from "../../api/client/sscs/factory/appeal.update.factory";
import issueDirectionTestdata from "../../pages/content/issue.direction_en.json";
import actionFurtherEvidenceTestdata from '../../pages/content/action.further.evidence_en.json';
import logger from "../../utils/loggerUtil";
import performAppealDormantOnCase from "../../api/client/sscs/appeal.event";

export class WriteFinalDecision extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performWriteFinalDecisionForAPIPAppealNoAwardAndNoticeGenerated(pipCaseId) {

        await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
        /*logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
        //await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Write final decision");

        //The Type of Appeal Page
        await this.writeFinalDecisionPage.verifyPageContentTypeOfAppealPage(true);
        await this.writeFinalDecisionPage.inputTypeOfAppealPageData(false, true); //No Awards but Generate Notice
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        //Allowed or Refused Page (Because we opted not in the previous page)
        await this.writeFinalDecisionPage.verifyPageContentAllowedOrRefusedPage();
        await this.writeFinalDecisionPage.chooseAllowedOrRefused("#writeFinalDecisionAllowedOrRefused-allowed");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentTypeOfHearingPage();
        await this.writeFinalDecisionPage.inputTypeOfHearingPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForPanelMembersPage('PIP');
        await this.writeFinalDecisionPage.inputPageContentForPanelMembersPageData('PIP');
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForDecisionDatePage();
        await this.writeFinalDecisionPage.inputTypePageContentForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForBundleSectionReferencePage();
        await this.writeFinalDecisionPage.inputPageContentForBundleSectionReferencePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForSummaryOutcomePage();
        await this.writeFinalDecisionPage.inputPageContentForSummaryOutcomePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReasonForDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForReasonForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForAnythingElseDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForAnythingElsePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(true);
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage.verifyPageContentForCheckYourAnswersPage();
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Write final decision");
    }

    async performWriteFinalDecisionForAPIPAppealAwardAndNoticeGenerated(pipCaseId) {

        await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
        /*logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
        //await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Write final decision");

        //The Type of Appeal Page
        await this.writeFinalDecisionPage.verifyPageContentTypeOfAppealPage(true);
        await this.writeFinalDecisionPage.inputTypeOfAppealPageData(true, true); //No Awards but Generate Notice
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        /*//Allowed or Refused Page (Because we opted not in the previous page)
        await this.writeFinalDecisionPage.verifyPageContentAllowedOrRefusedPage();
        await this.writeFinalDecisionPage.chooseAllowedOrRefused("#writeFinalDecisionAllowedOrRefused-allowed");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready*/

        await this.writeFinalDecisionPage.verifyPageContentTypeOfHearingPage();
        await this.writeFinalDecisionPage.inputTypeOfHearingPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyAndInputPageContentForTypeOfAwardPage();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyAndInputPageContentForAwardDatesPage();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyAndInputPageContentForAwardDatesPage();
        await this.writeFinalDecisionPage.inputPageContentForAwardDatesPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForPanelMembersPage('PIP');
        await this.writeFinalDecisionPage.inputPageContentForPanelMembersPageData('PIP');
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForDecisionDatePage();
        await this.writeFinalDecisionPage.inputTypePageContentForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForSelectActivitiesPage();
        await this.writeFinalDecisionPage.inputPageContentForSelectActivitiesPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForDailyLivingNutrition();
        await this.writeFinalDecisionPage.inputPageContentForDailyLivingNutritionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForMovingAround();
        await this.writeFinalDecisionPage.inputPageContentForMovingAroundPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForBundleSectionReferencePage();
        await this.writeFinalDecisionPage.inputPageContentForBundleSectionReferencePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReasonForDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForReasonForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForAnythingElseDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForAnythingElsePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(true);
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage.verifyPageContentForCheckYourAnswersPageForAwardsCriteria();
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Write final decision");
    }

    async performWriteFinalDecisionForATaxCreditAppealAndNoNoticeGenerated(taxCreditCaseId) {

        /*await this.loginUserWithCaseId(credentials.caseWorker, false, taxCreditCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Add a hearing');
        await this.addHearingPage.submitHearing();
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.clickSignOut();
        await this.homePage.delay(3000);*/


        /*await this.homePage.delay(3000);
        await this.homePage.chooseEvent('Hearing booked');
        await this.hearingBookedPage.submitHearingBooked();
        await this.homePage.clickSignOut();*/

        await this.loginUserWithCaseId(credentials.judge, false, taxCreditCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Write final decision");

        await this.writeFinalDecisionPage.verifyPageContentTypeOfAppealPage(false)
        await this.writeFinalDecisionPage.inputTypeOfAppealPageData(false, false, "TAX CREDIT"); //No Awards but Generate Notice
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        //Allowed or Refused Page (Because we opted not in the previous page)
        await this.writeFinalDecisionPage.verifyPageContentAllowedOrRefusedPage();
        await this.writeFinalDecisionPage.chooseAllowedOrRefused("#writeFinalDecisionAllowedOrRefused-refused");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(true);
        await this.writeFinalDecisionPage.inputPageContentForPreviewDecisionNoticePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage.verifyPageContentForCheckYourAnswersPageForNoNoticeGenerated();
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Write final decision");
    }

    async performWriteFinalDecisionForAUniversalCreditAppealAndNoticeGenerated(universalCreditCaseId :string) {

        /*await this.loginUserWithCaseId(credentials.caseWorker, false, taxCreditCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Add a hearing');
        await this.addHearingPage.submitHearing();
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.clickSignOut();
        await this.homePage.delay(3000);*/


        /*await this.homePage.delay(3000);
        await this.homePage.chooseEvent('Hearing booked');
        await this.hearingBookedPage.submitHearingBooked();
        await this.homePage.clickSignOut();*/

        await this.loginUserWithCaseId(credentials.judge, false, universalCreditCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Write final decision");

        await this.writeFinalDecisionPage.verifyPageContentTypeOfAppealPage(false)
        await this.writeFinalDecisionPage.inputTypeOfAppealPageData(false, true, "UNIVERSAL CREDIT"); //No Awards but Generate Notice
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        //Allowed or Refused Page (Because we opted not in the previous page)
        await this.writeFinalDecisionPage.verifyPageContentAllowedOrRefusedPage();
        await this.writeFinalDecisionPage.chooseAllowedOrRefused("#writeFinalDecisionAllowedOrRefused-allowed");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentTypeOfHearingPage();
        await this.writeFinalDecisionPage.inputTypeOfHearingPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForPanelMembersPage('UNIVERSAL CREDIT');
        await this.writeFinalDecisionPage.inputPageContentForPanelMembersPageData('UNIVERSAL CREDIT');
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForDecisionDatePage();
        await this.writeFinalDecisionPage.inputTypePageContentForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForWorkCapabilityAssessmentPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForWorkCapabilityAssessmentPageData(true)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForSchedule7ActivitiesPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule7ActivitiesPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForBundleSectionReferencePage();
        await this.writeFinalDecisionPage.inputPageContentForBundleSectionReferencePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReassessTheAwardPage();
        await this.writeFinalDecisionPage.inputPageContentForReassessTheAwardPage();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReasonForDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForReasonForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForAnythingElseDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForAnythingElsePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));


        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(true);
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));


        await this.writeFinalDecisionPage.verifyPageContentForCheckYourAnswersPageForUCCaseWithScheduleAndReasses();
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Write final decision");
    }

    async performIssueFinalDecisionForAnAppeal() {
        await this.homePage.chooseEvent("Issue final decision");
        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(false);
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Issue final decision");
    }

    async verifyFinalDecisionForAnAppeal() {
        await this.homePage.navigateToTab("Documents");
        await this.documentsTab.verifyPageContentByKeyValue("Type", "Final Decision Notice");
        await this.documentsTab.verifyPageContentByKeyValue("Bundle addition", "A");
        await this.documentsTab.verifydueDates("Date added");
    }

    async performWriteFinalDecisionForAESAAppealNoAwardGivenAndNoticeGenerated(esaCaseId :string) {

        await this.loginUserWithCaseId(credentials.judge, false, esaCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Write final decision");

        await this.writeFinalDecisionPage.verifyPageContentTypeOfAppealPage(false)
        await this.writeFinalDecisionPage.inputTypeOfAppealPageData(false, true, "ESA"); //No Awards but Generate Notice
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        // Allowed or Refused Page (Because we opted not in the previous page)
        await this.writeFinalDecisionPage.verifyPageContentAllowedOrRefusedPage();
        await this.writeFinalDecisionPage.chooseAllowedOrRefused("#writeFinalDecisionAllowedOrRefused-refused");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentTypeOfHearingPage();
        await this.writeFinalDecisionPage.inputTypeOfHearingPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForPanelMembersPage('ESA');
        await this.writeFinalDecisionPage.inputPageContentForPanelMembersPageData('ESA');
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForDecisionDatePage();
        await this.writeFinalDecisionPage.inputTypePageContentForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForWorkCapabilityAssessmentPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForWorkCapabilityAssessmentPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForSchedule2ActivitiesPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule2ActivitiesPageData("consciousness", "copingWithChange");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForConsciousnessTheAwardPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule2ConsciousnessPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForCopingTheAwardPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule2CopingPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentAndInputForRegulationPage();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForBundleSectionReferencePage();
        await this.writeFinalDecisionPage.inputPageContentForBundleSectionReferencePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReasonForDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForReasonForDecisionPageData();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForAnythingElseDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForAnythingElsePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));


        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(true);
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));


        await this.writeFinalDecisionPage
            .verifyPageContentForCheckYourAnswersPageForESACaseWithScheduleAndReasses(writeFinalDecisionData.refusedLabel);
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Write final decision");
    }


    async performWriteFinalDecisionForAESAAppealYesAwardGivenAndNoticeGenerated(esaCaseId :string) {

        await this.loginUserWithCaseId(credentials.judge, false, esaCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Write final decision");

        await this.writeFinalDecisionPage.verifyPageContentTypeOfAppealPage(false)
        await this.writeFinalDecisionPage.inputTypeOfAppealPageData(false, true, "ESA"); //No Awards but Generate Notice
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        // Allowed or Refused Page (Because we opted not in the previous page)
        await this.writeFinalDecisionPage.verifyPageContentAllowedOrRefusedPage();
        await this.writeFinalDecisionPage.chooseAllowedOrRefused("#writeFinalDecisionAllowedOrRefused-allowed");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentTypeOfHearingPage();
        await this.writeFinalDecisionPage.inputTypeOfHearingPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForPanelMembersPage('ESA');
        await this.writeFinalDecisionPage.inputPageContentForPanelMembersPageData('ESA');
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForDecisionDatePage();
        await this.writeFinalDecisionPage.inputTypePageContentForDecisionPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForWorkCapabilityAssessmentPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForWorkCapabilityAssessmentPageData(false)
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForSchedule2ActivitiesPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule2ActivitiesPageData("reaching", "learningTasks");
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReachingTheAwardPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule2ReachingPageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForCognitiveTheAwardPage();
        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule2CognitivePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.inputAndVerifyPageContentForSchedule3Activities();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForBundleSectionReferencePage();
        await this.writeFinalDecisionPage.inputPageContentForBundleSectionReferencePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReassessTheAwardPage();
        await this.writeFinalDecisionPage.inputPageContentForReassessTheAwardPage();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForReasonForDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForReasonForDecisionPageData();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready

        await this.writeFinalDecisionPage.verifyPageContentForAnythingElseDecisionPage();
        await this.writeFinalDecisionPage.inputPageContentForAnythingElsePageData();
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage.verifyPageContentForPreviewDecisionNoticePage(true);
        await this.writeFinalDecisionPage.submitContinueBtn();
        await new Promise(f => setTimeout(f, 1000));

        await this.writeFinalDecisionPage
            .verifyPageContentForCheckYourAnswersPageForESACaseWithScheduleAndReasses(writeFinalDecisionData.allowedLabel);
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Write final decision");
    }
}

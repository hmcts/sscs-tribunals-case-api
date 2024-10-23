import { Page, expect } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import eventTestData from '../../pages/content/event.name.event.description_en.json';
const uploadDocumentFurtherEvidenceData = require('../../pages/content/upload.document.further.evidence_en.json');
const actionFurtherEvidenceTestdata = require('../../pages/content/action.further.evidence_en.json');


export class ReissueFurtherEvidence extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }


    async performUploadDocumentFurtherEvidenceForReissueEvent(caseId: string, uploadAudioFile?: boolean) {

            await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
            await this.homePage.reloadPage();
            await expect(this.homePage.summaryTab).toBeVisible();
            await this.homePage.chooseEvent('Upload document FE');

            await this.uploadDocumentFurtherEvidencePage.verifyPageContent();
            await this.uploadDocumentFurtherEvidencePage.clickAddNew();
            await this.uploadDocumentFurtherEvidencePage.selectDocumenType(uploadDocumentFurtherEvidenceData.documentType);
            await this.uploadDocumentFurtherEvidencePage.inputFilename(uploadDocumentFurtherEvidenceData.fileName);

            if (uploadAudioFile) {
                await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc(uploadDocumentFurtherEvidenceData.testaudiofile);
            } else {
                await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc(uploadDocumentFurtherEvidenceData.testfileone);
            }

            await this.uploadDocumentFurtherEvidencePage.confirmSubmission();

            await this.eventNameAndDescriptionPage.verifyPageContent(uploadDocumentFurtherEvidenceData.eventName);
            await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
                eventTestData.eventDescriptionInput);
            await this.eventNameAndDescriptionPage.confirmSubmission();

            await expect(this.homePage.summaryTab).toBeVisible();
            await this.homePage.clickSignOut();
    }


    async performActionEvidence(caseId: string) {
        //selecting the event "Action further evidence"
        await this.goToActionFurtherEvidence(this.page, caseId);
        await this.reissueFurtherEvidencePage.verifyPageContentActionEvent();
        //completing the action further evidence
        await this.actionFurtherEvidencePage.selectFEOption();
        await this.actionFurtherEvidencePage.selectSenderOption(actionFurtherEvidenceTestdata.ftaSender);
        await this.actionFurtherEvidencePage.selectbundle();
        await this.actionFurtherEvidencePage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
    }


    async performReissueFurtherEvidence(caseId: string) {
        // starting the 'Reissue Further Evidence' event
        await this.goToReissueFurtherEvidence(this.page, caseId);
        await this.reissueFurtherEvidencePage.verifyPageContentReissueEvent();

        await this.reissueFurtherEvidencePage.applyReissueFurtherEvidence();
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        // Verifying History tab + end state
        await this.verifyHistoryTabDetails("Reissue further evidence");
        await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Reissue further evidence');
        await this.historyTab.verifyPageContentByKeyValue('Comment', 'Event Description for Automation Verification');

        await this.homePage.delay(1000); //reloading so we are able to verify 'update case only' in history tab
        await this.homePage.reloadPage();
        await this.verifyHistoryTabDetails("Update case only");
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Update case only');
        await this.historyTab.verifyPageContentByKeyValue('Comment', 'Update document evidence reissued flags after re-issuing further evidence to DWP');
    }

    // Event created to trigger Reissue Further Evidence event from next steps dropdown menu:
    private async goToReissueFurtherEvidence(page: Page, caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.chooseEvent("Reissue further evidence");
    }

    // Event created to trigger Action further evidence event from next steps dropdown menu:
    private async goToActionFurtherEvidence(page: Page, caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.chooseEvent("Action further evidence");
    }

}
import { BaseStep } from "./base";
import { Page } from '@playwright/test';
import {credentials} from "../../config/config";

const hearingRecordingTestData = require('../../pages/content/hearing.recording_en.json');


export class UploadHearing extends BaseStep {

    readonly page: Page;

    constructor(page){
        
        super(page);
        this.page = page;
    }

    async requestAndGrantAnHearingRecording(caseId: string) {
        
        await this.loginUserWithCaseId(credentials.caseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Add a hearing');
        await this.addHearingPage.submitHearing();
        await this.eventNameAndDescriptionPage.confirmSubmission();


        await this.homePage.delay(3000);
        await this.homePage.chooseEvent('Hearing booked');
        await this.hearingBookedPage.submitHearingBooked();
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 3000)); //Delay required for the Case to be ready

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.delay(60000); // wait for case update to happen
        await this.homePage.chooseEvent('Upload hearing recording');
        await this.uploadRecordingPage.selectRecording();
        await this.uploadRecordingPage.chooseHearingTypeAndAddRecording();
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab(hearingRecordingTestData.tabName);
        await this.hearingRecordingsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingTypeKey, hearingRecordingTestData.hearingTypeValue);
        await this.hearingRecordingsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingIDKey, hearingRecordingTestData.hearingIDValue);
        await this.hearingRecordingsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingVenueKey, hearingRecordingTestData.hearingVenueValue);
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 3000)); //Delay required for the Case to be ready

        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.chooseEvent('FTA Request hearing recording');
        await this.requestRecordingPage.selectRecordingForRequest();
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 3000)); //Delay required for the Case to be ready
        

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.chooseEvent('Action hearing recording req');
        await this.actionRecordingPage.grantRecordingRequest();
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Action hearing recording req');

        await this.homePage.navigateToTab('Documents');
        await this.documentsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingTypeKey, hearingRecordingTestData.hearingTypeValue);
        await this.documentsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingIDKey, hearingRecordingTestData.hearingIDValue);
        await this.documentsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingVenueKey, hearingRecordingTestData.hearingVenueValue);
        await this.documentsTab.verifyTitle('Released hearing recordings 1');
        await this.documentsTab.verifyPageContentByKeyValue(hearingRecordingTestData.requestPartyKey, hearingRecordingTestData.requestPartyValue);
        await this.documentsTab.verifydueDates('Date requested');
    }

    async requestAndRefuseAnHearingRecording(caseId: string) {
        
        await this.loginUserWithCaseId(credentials.caseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Add a hearing');
        await this.addHearingPage.submitHearing();
        await this.eventNameAndDescriptionPage.confirmSubmission();


        await this.homePage.delay(3000);
        await this.homePage.chooseEvent('Hearing booked');
        await this.hearingBookedPage.submitHearingBooked();
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 3000)); //Delay required for the Case to be ready

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.delay(60000); // wait for case update to happen
        await this.homePage.chooseEvent('Upload hearing recording');
        await this.uploadRecordingPage.selectRecording();
        await this.uploadRecordingPage.chooseHearingTypeAndAddRecording();
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab("Hearing Recordings");
        await this.hearingRecordingsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingTypeKey, hearingRecordingTestData.hearingTypeValue);
        await this.hearingRecordingsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingIDKey, hearingRecordingTestData.hearingIDValue);
        await this.hearingRecordingsTab.verifyPageContentByKeyValue(hearingRecordingTestData.hearingVenueKey, hearingRecordingTestData.hearingVenueValue);


        await this.homePage.chooseEvent('Upload document FE');
        await this.uploadDocumentPage.uploadFEDocument();
        await this.eventNameAndDescriptionPage.confirmSubmission();


        await this.homePage.chooseEvent('Action hearing recording req');
        await this.actionRecordingPage.refuseRecordingRequest();
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Action hearing recording req');

        await this.homePage.navigateToTab('Documents');
        await this.documentsTab.verifyPageContentNotPresentByKeyValue(hearingRecordingTestData.hearingTypeKey, hearingRecordingTestData.hearingTypeValue);
        await this.documentsTab.verifyPageContentNotPresentByKeyValue(hearingRecordingTestData.hearingIDKey, hearingRecordingTestData.hearingIDValue);
        await this.documentsTab.verifyPageContentNotPresentByKeyValue(hearingRecordingTestData.hearingVenueKey, hearingRecordingTestData.hearingVenueValue);
        await this.documentsTab.verifyTitleNotPresent('Released hearing recordings 1');
        await this.documentsTab.verifyPageContentNotPresentByKeyValue(hearingRecordingTestData.requestPartyKey, hearingRecordingTestData.requestPartyValue);
       
    }

}
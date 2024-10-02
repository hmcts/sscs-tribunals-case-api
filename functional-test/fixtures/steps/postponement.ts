import {Page} from '@playwright/test';
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import {BaseStep} from './base';
import {credentials} from "../../config/config";
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import performAppealDormantOnCase from "../../api/client/sscs/appeal.event";


export class Postponement extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async postponeAListAssistCaseWithAPostponement(grantRequest: string = 'Grant Postponement') {

        let pipCaseId = await createCaseBasedOnCaseType("PIP");

        /*await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
        logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        await this.homePage.delay(2000);
        await this.loginUserWithCaseId(credentials.caseWorker, false, pipCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Add a hearing');
        await this.addHearingPage.submitHearing('Hearing has been Listed');
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.delay(3000);

        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Hearing booked');
        await this.hearingBookedPage.submitHearingBooked();
        /*await this.homePage.delay(2000);
        await this.eventNameAndDescriptionPage.confirmSubmission();*/

        await this.homePage.delay(2000);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Postponement request');
        await this.postponementPage.verifyPageContentPostponementDetailsPage();
        await this.postponementPage.inputPostponementDetailsOfPageData();
        await this.postponementPage.submitContinueBtn();
        await this.postponementPage.verifyPageContentPreviewPostponementDocumentPage();
        await this.postponementPage.submitBtn();
        await this.eventNameAndDescriptionPage.verifyPageContent("Postponement request", false)
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Postponement request");

        await this.homePage.delay(2000);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Action Postponement Request');

        await this.postponementPage.verifyPageContentActionPostponementRequestPage(grantRequest);
        await this.postponementPage.inputPostponementActionRequestPageData(grantRequest);
        await this.postponementPage.submitBtn();
        await this.postponementPage.verifyPageContentActionPostponementRequestDocumentPage();
        await this.postponementPage.submitBtn();
        await this.homePage.delay(2000);

        //Could not Verify the Event Name and Description Page as the Event Summary Page Label is defined Differently.
        //await this.eventNameAndDescriptionPage.verifyPageContent("Action Postponement Request", false)
        if(grantRequest != 'Send to Judge') {
            await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
                eventTestData.eventDescriptionInput);
            await this.eventNameAndDescriptionPage.submitBtn();
            await this.homePage.delay(2000);
        }
        
        await this.homePage.navigateToTab("History");
        //await this.historyTab.verifyPageContentByKeyValue('End state', 'Hearing');
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Action Postponement Request');
        await this.historyTab.verifyEventCompleted('Action Postponement Request');
        //await performAppealDormantOnCase(pipCaseId);
    }
}

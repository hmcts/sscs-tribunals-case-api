import {Page} from '@playwright/test';
import {BaseStep} from './base';
import {credentials} from "../../config/config";

const appointeeDetailsData = require("../../pages/content/appointee.details_en.json");
const eventTestData = require("../../pages/content/event.name.event.description_en.json");


export class ProvideAppointeeDetails extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performAttemptAppointeeDetailsNonDormantCase(caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.provideAppointeeDetailsPage.verifyDropDownElementNotVisble(appointeeDetailsData.eventName);
    }

    async performNoAppointeeDetails(caseId: string) {
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.chooseEvent(appointeeDetailsData.eventName);
        
        await this.provideAppointeeDetailsPage.verifyPageContent(appointeeDetailsData.eventName);
        await this.provideAppointeeDetailsPage.chooseAssistOption(appointeeDetailsData.appointeeDetailsNoLabel);
        await this.provideAppointeeDetailsPage.continueSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(appointeeDetailsData.eventName);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.verifyHistoryTabDetails(appointeeDetailsData.endState, appointeeDetailsData.eventName);
    }

    async performProvideAppointeeDetails(caseId: string) {
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.chooseEvent(appointeeDetailsData.eventName);

        await this.provideAppointeeDetailsPage.verifyPageContent(appointeeDetailsData.eventName);
        await this.provideAppointeeDetailsPage.chooseAssistOption(appointeeDetailsData.appointeeDetailsYesLabel);
        await this.provideAppointeeDetailsPage.verifyAndPopulateAppointeeDetailsPage(appointeeDetailsData);
        await this.provideAppointeeDetailsPage.continueSubmission()

        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.clickSignOut();


        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifyAppealDetailsAppointeeDetails(appointeeDetailsData);

        await this.verifyHistoryTabDetails(appointeeDetailsData.endState, appointeeDetailsData.eventName);
    }

}

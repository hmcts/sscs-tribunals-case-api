import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import sendToAdminData from '../../pages/content/send.to.admin_en.json';
const eventTestData = require("../../pages/content/event.name.event.description_en.json");


export class SendToAdmin extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performSendToAdmin(caseId: string) {

        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Send to admin');
        //await this.homePage.waitForLoadState();

        await this.comepleteSendToAdmin();
    }

    async comepleteSendToAdmin() {
        //Params are passed to this page as this is a common page to be reused.
        await this.textAreaPage.verifyPageContent(sendToAdminData.sendToAdminCaption,
            sendToAdminData.sendToAdminHeading,
            sendToAdminData.sendToAdminFieldLabel);
        await this.textAreaPage.inputData(sendToAdminData.sendToAdminInput);
        await this.textAreaPage.confirmSubmission();

        //Params are passed to this page as this is a common page to be reused.
        await this.eventNameAndDescriptionPage.verifyPageContent('Send to admin',true,
            sendToAdminData.sendToAdminFieldLabel, sendToAdminData.sendToAdminInput);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.verifyHistoryTabDetails('With FTA', 'Send to admin', 'Event Description for Automation Verification');
    }
}

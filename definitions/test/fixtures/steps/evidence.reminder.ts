import {Page} from '@playwright/test';
import {BaseStep} from './base';
import {credentials} from '../../config/config';

const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class EvidenceReminder extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performEvidenceReminder(caseId: string) {

        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Evidence reminder');
        await this.eventNameAndDescriptionPage.verifyPageContent("Evidence reminder");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.navigateToTab("History");
        await this.verifyHistoryTabLink('Evidence reminder');

        //await this.verifyHistoryTabDetails('With FTA', 'Evidence reminder', 'Event Description for Automation Verification');
        await this.verifyHistoryTabDetails('Valid Appeal', 'Evidence reminder', 'Event Description for Automation Verification');
    }
}

import { Page, expect } from '@playwright/test';
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import { BaseStep } from './base';
import { credentials } from "../../config/config";

export class MarkCaseAsUrgent extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performMarkCaseAsUrgent(caseId: string, loginRequired: boolean = true): Promise<void>{

        if (loginRequired) {
            await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
            await this.homePage.reloadPage();
        }

        await this.homePage.chooseEvent('Mark case as urgent');

        await this.eventNameAndDescriptionPage.verifyPageContent('Mark case as urgent');
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.verifyHistoryTabDetails('With FTA', 'Mark case as urgent', eventTestData.eventDescriptionInput);
    }
}

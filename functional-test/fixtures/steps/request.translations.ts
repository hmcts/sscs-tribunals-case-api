import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import eventTestData from '../../pages/content/event.name.event.description_en.json';

export class RequestTranslations extends BaseStep {
    
  readonly page : Page;

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performRequestTranslations(caseId :string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Welsh - request translations');

        await this.completeRequestTranslations();
    }

    async completeRequestTranslations() {
        await this.eventNameAndDescriptionPage.verifyPageContent('Welsh - request translations');
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput, eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab('History');
        await this.historyTab.verifyEventCompleted('Welsh - request translations');
    }
}

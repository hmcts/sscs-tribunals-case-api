import { Page } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class StrikeOutCase extends BaseStep {
    
  readonly page : Page;
  

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performStrikeOutCase(caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.chooseEvent('Strike out case');
        await this.eventNameAndDescriptionPage.verifyPageContent("Strike out case");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails('Dormant', 'Strike out case', 'Event Description for Automation Verification');
    }
}

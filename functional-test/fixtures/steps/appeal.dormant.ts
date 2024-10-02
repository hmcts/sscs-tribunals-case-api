import { Page } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class AppealDormant extends BaseStep {
    
  readonly page : Page;
  

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performAppealDormant(caseId: string) {
        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        await this.homePage.chooseEvent('Appeal dormant');

        await this.eventNameAndDescriptionPage.verifyPageContent("Appeal dormant");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.verifyHistoryTabDetails("Dormant", 'Appeal dormant', 'Event Description for Automation Verification');
    }
}

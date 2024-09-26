import { Page } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class SendToDormant extends BaseStep {
    
  readonly page : Page;
  

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performSendToDormant(caseId: string) {
        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        await this.homePage.chooseEvent('Admin - send to Dormant');

        await this.eventNameAndDescriptionPage.verifyPageContent("Admin - send to Dormant");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.verifyHistoryTabDetails("Dormant", 'Admin - send to Dormant', 'Event Description for Automation Verification');
    }
}

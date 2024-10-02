import { Page, expect } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class VoidCase extends BaseStep {
    
  readonly page : Page;
  

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performVoidCase(caseId: string, loginRequired: boolean = true): Promise<void> {
        
        if(loginRequired) {
            await this.loginUserWithCaseId(credentials.amCaseWorker, false ,caseId);
            await this.homePage.reloadPage(); 
        }
        await this.homePage.chooseEvent('Void case');
        await this.eventNameAndDescriptionPage.verifyPageContent("Void case");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.verifyHistoryTabDetails("Dormant", 'Void case', 'Event Description for Automation Verification');
    }
}

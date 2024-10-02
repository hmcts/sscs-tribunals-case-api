import { Page, expect } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class AppealWithdrawn extends BaseStep {

  readonly page : Page;


   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performAppealWithdrawn(caseId: string, loginRequired: boolean = true) {

        if(loginRequired) {
            await this.loginUserWithCaseId(credentials.amSuperUser, false ,caseId);
            await this.homePage.reloadPage(); 
        }

        await this.homePage.chooseEvent("Appeal withdrawn");

        await this.eventNameAndDescriptionPage.verifyPageContent("Appeal withdrawn");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.verifyHistoryTabDetails("Dormant");
    }
}

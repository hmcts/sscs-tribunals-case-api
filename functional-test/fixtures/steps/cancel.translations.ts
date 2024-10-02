import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import eventTestData from '../../pages/content/event.name.event.description_en.json';

export class CancelTranslations extends BaseStep {
    
  readonly page : Page;

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performCancelTranslations(caseId :string, loginRequired :boolean = true) : Promise<void>{

        if(loginRequired) {
            await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
            await this.homePage.reloadPage(); 
        }

        await this.homePage.chooseEvent('Welsh - cancel translations');

        await this.eventNameAndDescriptionPage.verifyPageContent('Welsh - cancel translations');
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput, eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab('History');
        await this.historyTab.verifyEventCompleted('Welsh - cancel translations');
    }
}

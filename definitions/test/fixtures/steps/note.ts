import { Page } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class Note extends BaseStep {
    
  readonly page : Page;
  

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performAddANote(caseId :string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Add a note');

        /* speak with Pettedson regarding test step 38 */
        //await addNotePage.verifyPageContent(StringUtilsComponent.formatClaimReferenceToAUIDisplayFormat(pipCaseId));
        await this.addNotePage.inputData();
        await this.addNotePage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent('Add a note', false , null, null);
        //Params are passed to this page as this is a common page to be reused.
        await this.eventNameAndDescriptionPage.inputData(`${eventTestData.eventSummaryInput} - Add a note`,
            `${eventTestData.eventDescriptionInput} - Add a note`);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab("Notepad");
        await this.notePadTab.verifyPageContentByKeyValue('Note','Playwright test note');
        await this.homePage.navigateToTab("History");
        await this.historyTab.verifyEventCompleted("Add a note");
    }
}

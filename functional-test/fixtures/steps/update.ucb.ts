import { BaseStep } from "./base";
import { Page } from '@playwright/test';

const ucbTestData = require('../../pages/content/update.ucb_en.json');


export class UpdateUCB extends BaseStep {

    readonly page: Page;

    constructor(page){
        
        super(page);
        this.page = page;
    }

    async verifyUpdatedUCBOption() {
        
        await this.homePage.chooseEvent(ucbTestData.eventNameCaptor);
        await this.updateUCBPage.verifyPageContent();
        await this.updateUCBPage.updateUCBToNo();
        await this.updateUCBPage.confirmSubmission();
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.listingRequirementsTab.verifyContentNotPresent(ucbTestData.ucbFieldLabel, ucbTestData.ucbFieldValue_Yes);

        await this.homePage.navigateToTab("Summary");
        await this.summaryTab.verifyPresenceOfText("Ready to list");   
    }
}
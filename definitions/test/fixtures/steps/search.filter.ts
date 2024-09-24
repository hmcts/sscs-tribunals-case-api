import { Page, expect } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";

let caseId: string;
let caseId2: string;

export class SearchFilter extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performSearchSteps() {
        caseId = await createCaseBasedOnCaseType('PIP');
        caseId2 = await createCaseBasedOnCaseType('PIP');

        await this.goToCaseListPage(this.page);
      
        await this.searchFilterPage.performSearch();
       
        await this.searchFilterPage.validateSearchResults;
    }
    
    // Event created to go to the Case list page from the heading link:
    private async goToCaseListPage(page: Page) {
        await this.loginUserWithoutCaseId(credentials.amCaseWorker, true);
        await this.homePage.goToCaseList();
    }
}
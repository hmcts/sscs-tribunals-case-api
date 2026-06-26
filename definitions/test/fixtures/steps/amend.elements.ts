import { BaseStep } from "./base";
import { expect, Page } from '@playwright/test';
import { credentials } from '../../config/config';

export class AmendElements extends BaseStep {

     readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performAmendElements(caseId: string) {

        await this.signOut();
        await this.loginUserWithCaseId(credentials.caseWorker, false, caseId);
        await this.homePage.chooseEvent('Amend elements/issues');
        await this.amendElementPage.updateElementAndIssueCode();

        await this.homePage.navigateToTab('Elements and issues');
        await this.elementsAndIssuesTab.verifyPageContentByKeyValue('Childcare 1', 'GC');
        await this.elementsAndIssuesTab.verifyPageContentByKeyValue('Child element 1', 'WC');
    }
}
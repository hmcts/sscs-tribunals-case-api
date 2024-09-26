import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
const uploadResponseTestdata = require('../pages/content/upload.response_en.json');

let webActions: WebAction;

export class RequestTimeExtensionPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Request time extension'); 
    }

    async uploadTimeExtensionDoc(): Promise<void> {
        await webActions.uploadFileUsingAFileChooser('#tl1Form_documentLink', uploadResponseTestdata.testfileone);
        await this.page.waitForTimeout(7000);
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
        await this.page.waitForTimeout(2000);
    } 
}
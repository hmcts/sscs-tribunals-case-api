import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webAction: WebAction;

export class ReviewPHEPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('h1.govuk-heading-l', 'Review PHE request'); //Heading Text
        await webAction.verifyPageLabel('span.form-label', 'Should the potentially harmful evidence be excluded?'); //Field Label
    }

    async selectGrantPermission() {
        await webAction.clickElementById('#phmeGranted_Yes');
    }

    async selectRefusePermission() {
        await webAction.clickElementById('#phmeGranted_No');
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}
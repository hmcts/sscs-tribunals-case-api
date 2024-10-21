import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class ResponseReviewedPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent(captionValue: string, headingValue:string) {

        //.govuk-caption-l
        await webActions.verifyPageLabel('.govuk-caption-l', captionValue); //Caption Text
        await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
    }

    async chooseInterlocOption(radioValue: string): Promise<void> {
        await webActions.clickElementById(`#isInterlocRequired_${radioValue}`);
    }

    async continueSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await webActions.clickButton('Continue');
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
    }

}
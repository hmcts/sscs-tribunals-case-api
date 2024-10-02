import { expect, Locator, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webAction: WebAction;

export class CreateBundlePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('.govuk-heading-l', "Create a bundle"); //Heading Text
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    }

    async cancelEvent(): Promise<void> {
        await webAction.clickLink("Cancel");
    }
 
}
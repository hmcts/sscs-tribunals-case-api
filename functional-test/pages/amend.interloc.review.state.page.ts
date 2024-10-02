import { Page, expect } from '@playwright/test';
import { WebAction } from '../common/web.action';
import amendInterlocReviewStateData from "./content/amend.interloc.review.state_en.json";

let webAction: WebAction;

export class AmendInterlocReviewStatePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('span.govuk-caption-l', amendInterlocReviewStateData.caption); //Captor Text
        await webAction.verifyPageLabel('h1.govuk-heading-l', amendInterlocReviewStateData.heading); //Heading Text
        await webAction.verifyPageLabel('label[for="interlocReviewState"]', amendInterlocReviewStateData.interlocReviewStateDropdownLabel); //Field Label
    }

    async selectReviewState(option: string): Promise<void> {
        await webAction.chooseOptionByLabel('#interlocReviewState', option);
    }

    async confirmSelection(): Promise<void> {
        await webAction.clickButton('Submit');
        await expect(this.page.locator('#interlocReviewState')).toBeHidden();
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    }
}
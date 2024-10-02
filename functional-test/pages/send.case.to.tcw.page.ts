import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import sendCaseToTcwData from "./content/send.case.to.tcw_en.json";

let webAction: WebAction;

export class SendCaseToTcwPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('span.govuk-caption-l', sendCaseToTcwData.caption); // Caption text
        await webAction.verifyPageLabel('h1.govuk-heading-l', sendCaseToTcwData.heading); //Heading Text
        await webAction.verifyPageLabel('label[for=\'interlocReviewState\'] span', sendCaseToTcwData.interlocReviewStateDropdownLabel); //Field Label
        await webAction.verifyPageLabel('label[for=\'tempNoteDetail\']', sendCaseToTcwData.enterNoteFieldLabel);
    }

    async enterNote(text: string): Promise<void> {
        await webAction.inputField('#tempNoteDetail', text);
        await this.page.locator('label[for=\'tempNoteDetail\']').click();
    }

    async selectInterlocutoryReviewState(reviewState: string): Promise<void> {
        await webAction.chooseOptionByLabel('#interlocReviewState', reviewState);
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}
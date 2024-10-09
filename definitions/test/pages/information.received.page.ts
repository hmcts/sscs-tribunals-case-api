import { Page } from '@playwright/test';
import informationReceivedData from "./content/information.received_en.json"
import { WebAction } from '../common/web.action';

let webAction: WebAction;

export class InformationReceivedPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('span.govuk-caption-l', informationReceivedData.informationReceivedCaption); //Captor Text
        await webAction.verifyPageLabel('h1.govuk-heading-l', informationReceivedData.informationReceivedHeading); //Heading Text
        await webAction.verifyPageLabel('label[for="interlocReviewState"]', informationReceivedData.informationReceivedFieldLabel); //Field Label
    }

    async selectReviewState(option: string): Promise<void> {
        await webAction.chooseOptionByLabel('#interlocReviewState', option);
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    }
}

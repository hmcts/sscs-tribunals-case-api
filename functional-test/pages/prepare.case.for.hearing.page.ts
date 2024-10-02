import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import prepareCaseForHearingData from "./content/prepare.case.for.hearing_en.json";

let webAction: WebAction;

export class PrepareCaseForHearingPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('h1.govuk-heading-l', prepareCaseForHearingData.pageHeading); //Heading Text
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}
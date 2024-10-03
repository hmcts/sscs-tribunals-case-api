import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webAction: WebAction;
const reviewConfidentialityTestdata = require('./content/review.confidentiality_en.json');

export class ReviewConfidentialityPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContentForReviewConfPage() {

        await webAction.verifyPageLabel('.govuk-caption-l', reviewConfidentialityTestdata.eventNameCaptor);
        await webAction.verifyPageLabel('h1.govuk-heading-l', reviewConfidentialityTestdata.eventNameCaptor);
    }


    async selectGrantConfidentialityForAppellant() {
        await webAction.clickElementById('#confidentialityRequestAppellantGrantedOrRefused-grantConfidentialityRequest');
        await webAction.clickButton('Continue');
    }

    async selectRefuseConfidentialityForJP() {
        await webAction.clickElementById('#confidentialityRequestJointPartyGrantedOrRefused-refuseConfidentialityRequest');
        await webAction.clickButton('Continue');
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}
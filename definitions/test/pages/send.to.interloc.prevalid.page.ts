import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import sendToInterLocPreValidData from "./content/send.to.interloc_en.json";

let webAction: WebAction;

export class SendToInterlocPrevalidPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContentForTheInterlocReferralPage() {
        //.govuk-caption-l
        await webAction.verifyPageLabel('.govuk-caption-l', sendToInterLocPreValidData.sendToInterLocPreValidCaption);
        await webAction.verifyPageLabel('h1.govuk-heading-l', sendToInterLocPreValidData.sendToInterLocPageHeading);
        await webAction.verifyPageLabel('.form-label', sendToInterLocPreValidData.sendToInterLocReasonOptionLabel);
    }

    async inputReasonForReferral(): Promise<void> {
        await webAction.chooseOptionByLabel('#interlocReferralReason', sendToInterLocPreValidData.sendToInterLocReferalReasonOptionValue);
    }

    async submitContinueBtn(): Promise<void> {
        await webAction.clickButton("Continue");
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}

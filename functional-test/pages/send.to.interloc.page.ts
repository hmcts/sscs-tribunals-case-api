import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import sendToInterlocData from "./content/send.to.interloc_en.json";

let webAction: WebAction;

export class SendToInterlocPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('h1.govuk-heading-l', sendToInterlocData.sendToInterlocHeading); //Heading Text
        await webAction.verifyPageLabel('label[for=\'prePostHearing\']', sendToInterlocData.sendToInterlocPrePostHearingFieldLabel); //Field Label
        await webAction.verifyPageLabel('label[for=\'selectWhoReviewsCase\']', sendToInterlocData.sendToInterlocCaseReviewFieldLabel);
        await webAction.verifyPageLabel('label[for=\'interlocReferralReason\']', sendToInterlocData.sendToInterlocReasonReferredFieldLabel);
    }

    async selectHearingType(hearingtype: string): Promise<void> {
        await webAction.chooseOptionByLabel('#prePostHearing', hearingtype);
    }

    async selectCaseReview(caseReview: string): Promise<void> {
        await webAction.chooseOptionByLabel('#selectWhoReviewsCase', caseReview);
    }

    async selectReasonReferred(reasonReferred: string): Promise<void> {
        await webAction.chooseOptionByLabel('#interlocReferralReason', reasonReferred);
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}
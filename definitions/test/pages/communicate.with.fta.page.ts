import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class CommunicateWithFtaPage {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent(isCommsToFta: boolean) {
        await webActions.verifyPageLabel('.form-label.ng-star-inserted', 'Communication type');
        if(isCommsToFta){
            await webActions.verifyPageLabel('.govuk-caption-l.ng-star-inserted', 'Communication with FTA');
            await webActions.verifyPageLabel('label.form-label',
                [
                    'New Request',
                    'Reply to FTA Query',
                    'Review FTA Reply',
                    'Delete a request/reply'
                ]);
        } else {
            await webActions.verifyPageLabel('.govuk-caption-l.ng-star-inserted', 'Communication with Tribunal');
            await webActions.verifyPageLabel('label.form-label',
                [
                    'New Request',
                    'Reply to Tribunal Query',
                    'Review Tribunal Reply'
                ]);
        }
    }

    async selectCommunicationType(communicationType: string) {
        await webActions.clickRadioButton(communicationType);
        await webActions.clickButton('Continue');
    }

    async fillOutNewRequestData(requestTopic: string, userType: string) {
        await webActions.chooseOptionByLabel('#commRequestTopic', requestTopic);
        await webActions.inputField('#commRequestQuestion', `Test details for ${userType}  communication Request`);
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }

    async fillOutReviewFtaReply() {
        await webActions.clickRadioButton('Review FTA Reply');
        await webActions.clickButton('Continue');
        const tribunalComms = await this.page.locator("//input[@name='tribunalRequestsToReviewDl']/../label//p").textContent();
        await webActions.checkAnCheckBox(tribunalComms);
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }

    async fillOutReviewTribunalReply() {
        await webActions.clickRadioButton('Review Tribunal Reply');
        await webActions.clickButton('Continue');
        const tribunalComms = await this.page.locator("//input[@name='ftaRequestsToReviewDl']/../label//p").textContent();
        await webActions.checkAnCheckBox(tribunalComms);
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }

    async deleteRequestOrReply() {
        await webActions.clickRadioButton('Delete a request/reply');
        await webActions.clickButton('Continue');
        await webActions.chooseOptionByIndex('#deleteCommRequestRadioDl', 1);
        await webActions.clickButton('Continue');
        await webActions.inputField('#deleteCommRequestTextArea', 'Test reason for deleting request/reply');
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }
}
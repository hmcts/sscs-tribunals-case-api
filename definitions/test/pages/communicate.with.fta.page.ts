import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class CommunicateWithFtaPage {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('.form-label.ng-star-inserted', 'Communication type');
        await webActions.verifyPageLabel('.govuk-caption-l.ng-star-inserted', 'Communication with FTA');
        await webActions.verifyPageLabel('label.form-label',
            [
                'New Request',
                'Reply to FTA Query',
                'Review FTA Reply',
                'Delete a request/reply'
            ]);
    }

    async selectCommunicationType(communicationType: string) {
        await webActions.clickRadioButton(communicationType);
        await webActions.clickButton('Continue');
    }

    async fillOutRequestData(requestTopic: string) {
        await webActions.chooseOptionByLabel('#commRequestTopic', requestTopic);
        await webActions.inputField('#commRequestQuestion', 'Test details for FTA communication Request');
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }
}
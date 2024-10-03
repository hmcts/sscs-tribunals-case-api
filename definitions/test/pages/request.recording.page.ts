import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';

let webActions: WebAction;

export class RequestRecordingPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async selectRecordingForRequest(): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Request hearing recording');
        // await webActions.verifyTextVisibility('Outstanding hearing recording requests');
        // await webActions.verifyTextVisibility('Released hearing recordings');
        // await webActions.verifyElementVisibility('#requestableHearingDetails');
        await webActions.chooseOptionByLabel('#requestableHearingDetails', 'Fox court 13:00 20 Feb 2024');
        await webActions.clickButton('Continue');
        // await webActions.clickSubmitButton();
    }
}
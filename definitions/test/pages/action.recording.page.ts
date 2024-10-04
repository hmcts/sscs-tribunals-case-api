import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';

let webActions: WebAction;

export class ActionRecordingPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async grantRecordingRequest(): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Action hearing recording request');
        await webActions.chooseOptionByLabel('#selectHearingDetails', 'Fox court 13:00:00 20 Feb 2024');
        await webActions.clickButton('Continue');

        await webActions.verifyTextVisibility('Hearing 1');
        await webActions.verifyTextVisibility('Fox court 13:00:00 20 Feb 2024');
        await webActions.chooseOptionByLabel('#processHearingRecordingRequest_dwp', 'Granted');
        await webActions.clickSubmitButton();
    }

    async refuseRecordingRequest(): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Action hearing recording request');
        await webActions.chooseOptionByLabel('#selectHearingDetails', 'Fox court 13:00:00 20 Feb 2024');
        await webActions.clickButton('Continue');

        await webActions.verifyTextVisibility('Hearing 1');
        await webActions.verifyTextVisibility('Fox court 13:00:00 20 Feb 2024');
        await webActions.chooseOptionByLabel('#processHearingRecordingRequest_appellant', 'Refused');
        await webActions.clickSubmitButton();
    }
}
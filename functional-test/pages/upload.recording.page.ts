import {Page, expect} from '@playwright/test';
import {WebAction} from '../common/web.action';

let webActions: WebAction;

export class UploadRecordingPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async selectRecording(): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Upload hearing recording');
        await webActions.chooseOptionByLabel('#selectHearingDetails', 'Fox court 13:00:00 20 Feb 2024');
        await webActions.clickButton('Continue');
    }

    async chooseHearingTypeAndAddRecording(): Promise<void> {
        await webActions.clickRadioButton('Final Hearing');
        await webActions.clickButton('Add new');

        await expect(async() => {
            await webActions.uploadFileUsingAFileChooser('#hearingRecording_recordings_value', 'test_av.mp3');
            await webActions.delay(7000);
        }).toPass();
        await webActions.clickButton('Continue');
    }
}
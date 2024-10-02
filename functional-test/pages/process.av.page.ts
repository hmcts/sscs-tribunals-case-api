import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';

let webActions: WebAction;

export class ProcessAVPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async selectRequestedEvidence(filename: string): Promise<void> {
        await webActions.delay(5000);
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Process Audio/Video Evidence');
        await webActions.chooseOptionByLabel('#selectedAudioVideoEvidence', filename);
        await webActions.clickButton('Continue');
    }

    async verifyPageContent(partyType: string): Promise<void> {
        await webActions.delay(5000);
        await webActions.verifyTextVisibility('Audio document');
        await webActions.verifyTextVisibility('test_av.mp3');
        await webActions.verifyTextVisibility(partyType);
        await webActions.verifyTextVisibility('Statement of evidence pdf');
    }

    async grantApprovalEvidence(): Promise<void> {
        await webActions.delay(5000);
        await webActions.chooseOptionByLabel('#processAudioVideoAction', 'Admit audio/video evidence');
        await webActions.inputField('#directionNoticeContent', 'This is a test direction');
        await webActions.inputField('#signedBy', 'Test user');
        await webActions.inputField('#signedRole', 'Tester');

        await webActions.chooseOptionByLabel('#processAudioVideoReviewState', 'Awaiting Admin Action');
        await webActions.clickButton('Continue');
    }

    async rejectApprovalEvidence(): Promise<void> {
        await webActions.delay(5000);
        await webActions.chooseOptionByLabel('#processAudioVideoAction', 'Exclude audio/video evidence');
        await webActions.inputField('#directionNoticeContent', 'This is a test direction');
        await webActions.inputField('#signedBy', 'Test user');
        await webActions.inputField('#signedRole', 'Tester');

        await webActions.chooseOptionByLabel('#processAudioVideoReviewState', 'Awaiting Admin Action');
        await webActions.clickButton('Continue');
    }

    async continueOnPreviewDoc(): Promise<void> {
        await webActions.delay(3000);
        await webActions.verifyTextVisibility('Preview Document');
        await webActions.clickSubmitButton();
        await webActions.delay(2000);
        await webActions.clickSubmitButton();
        await webActions.delay(3000);
    }
}
import {Page, expect} from '@playwright/test';
import {WebAction} from '../common/web.action';

let webActions: WebAction;

export class UploadDocumentPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async uploadFEDocument() {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Upload further evidence documents');
        await webActions.clickButton('Add new');
        await webActions.chooseOptionByLabel('#draftSscsFurtherEvidenceDocument_0_documentType', 'Request for Hearing Recording');
        await webActions.inputField('#draftSscsFurtherEvidenceDocument_0_documentFileName', 'testfile');
        await webActions.uploadFileUsingAFileChooser('#draftSscsFurtherEvidenceDocument_0_documentLink', 'testfile1.pdf');
        await webActions.delay(3000);
        await webActions.clickSubmitButton();

        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Hearing request party');
        await webActions.chooseOptionByLabel('#requestingParty', 'Appellant (or Appointee)');
        await webActions.clickButton('Continue');

        await webActions.chooseOptionByLabel('#requestableHearingDetails', 'Fox court 13:00 20 Feb 2024');
        await webActions.clickSubmitButton();

        // await webActions.clickSubmitButton();
    }
}
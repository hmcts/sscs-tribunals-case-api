import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import { timingSafeEqual } from 'crypto';
const uploadResponseTestdata = require('../pages/content/upload.response_en.json');

let webActions: WebAction;

export class UploadResponsePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        // await webActions.verifyPageLabel('.govuk-caption-l', 'Upload response'); //Captor Text
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Upload response'); //Page heading
        //await webActions.verifyPageLabel('h1', casereference+": Bloggs"); //Captor Text
       /* await webActions.verifyPageLabel('h2', 'FTA Response'); //Section heading
        await webActions.verifyPageLabel('h2', 'AT38 (Optional)'); //Section heading
        await webActions.verifyPageLabel('h2', 'FTA Evidence bundle'); //Section heading
        await webActions.verifyPageLabel('h2', 'Audio/Video Evidence'); //Section heading */
        await webActions.isLinkClickable('Cancel');
    }

    async uploadDocs(): Promise<void> {
        await webActions.uploadFileUsingAFileChooser('#dwpResponseDocument_documentLink', uploadResponseTestdata.testfileone);
        await this.page.waitForTimeout(9000);
        await webActions.uploadFileUsingAFileChooser('#dwpAT38Document_documentLink', uploadResponseTestdata.testfiletwo);
        await this.page.waitForTimeout(9000);
        await webActions.uploadFileUsingAFileChooser('#dwpEvidenceBundleDocument_documentLink', uploadResponseTestdata.testfilethree);
        await this.page.waitForTimeout(7000);
    }

    async uploadPartialDocs(): Promise<void> {
        await webActions.uploadFileUsingAFileChooser('#dwpResponseDocument_documentLink', uploadResponseTestdata.testfileone);
        await this.page.waitForTimeout(7000);
        await webActions.uploadFileUsingAFileChooser('#dwpEvidenceBundleDocument_documentLink', uploadResponseTestdata.testfilethree);
        await this.page.waitForTimeout(7000);
    }

    async uploadPHEDocs(): Promise<void> {
        await webActions.chooseOptionByLabel('#dwpEditedEvidenceReason', 'Potentially harmful evidence');
        await webActions.uploadFileUsingAFileChooser('#dwpEditedResponseDocument_documentLink', uploadResponseTestdata.testfileone);
        await this.page.waitForTimeout(10000);
        await webActions.uploadFileUsingAFileChooser('#dwpEditedEvidenceBundleDocument_documentLink', uploadResponseTestdata.testfiletwo);
        await this.page.waitForTimeout(7000);
        await webActions.uploadFileUsingAFileChooser('#appendix12Doc_documentLink', uploadResponseTestdata.testfilethree);
        await this.page.waitForTimeout(7000);
    }

    async uploadAVDocs(): Promise<void> {
        await this.clickAddNewButton();
        await webActions.uploadFileUsingAFileChooser('#dwpUploadAudioVideoEvidence_0_documentLink', uploadResponseTestdata.testaudiofile);
        await this.page.waitForTimeout(10000);
        await webActions.uploadFileUsingAFileChooser('#dwpUploadAudioVideoEvidence_0_rip1Document', uploadResponseTestdata.testfiletwo);
        await this.page.waitForTimeout(7000);
    }

    async uploadUCBDocs(): Promise<void> {
        await webActions.clickElementById('#dwpUCB_Yes');
        await webActions.uploadFileUsingAFileChooser('#dwpUcbEvidenceDocument', uploadResponseTestdata.testfileone);
        await this.page.waitForTimeout(10000);
    }

    async verifyDocMissingErrorMsg(): Promise<void>{
        //await webActions.screenshot();
        await webActions.verifyElementVisibility('#errors');
        await webActions.verifyTextVisibility('AT38 document is missing');
    }

    async selectIssueCode(issueCode: string): Promise<void> {
        await webActions.chooseOptionByLabel('#issueCode', issueCode);
    }

    async selectEvidenceReason(optionVal: string): Promise<void> {
        await webActions.chooseOptionByLabel('#dwpEditedEvidenceReason', optionVal);
    }

    async chooseAssistOption(optionVal: string): Promise<void> {
        await webActions.clickElementById(`#dwpFurtherInfo_${optionVal}`);
    }

    async selectElementDisputed(optionVal: string): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Elements disputed'); //Heading Text
        await webActions.clickElementById(`#elementsDisputedList-${optionVal}`);
    }

    async clickAddNewButton(): Promise<void> {
        await webActions.clickButton('Add new');
    }

    async selectUcIssueCode(issueCode: string): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Issue codes'); //Heading Text
        await webActions.chooseOptionByLabel('#elementsDisputedChildElement_0_issueCode', issueCode);
    }

    async chooseDisputeOption(optionVal: string):Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Disputed'); //Heading Text
        await webActions.clickElementById(`#elementsDisputedIsDecisionDisputedByOthers_${optionVal}`);
    }

    async isJPOnTheCase(optionVal: string):Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Joint party'); //Heading Text
        await webActions.clickElementById(`#jointParty_${optionVal}`);
    }

    async enterJPDetails() {
        await webActions.chooseOptionByLabel('#jointPartyName_title', 'Mr');
        await webActions.typeField('#jointPartyName_firstName', "fname");
        await webActions.typeField('#jointPartyName_lastName', "lname");
        await webActions.clickButton('Continue');
        await webActions.typeField('#dob-day', '20');
        await webActions.typeField('#dob-month', '5');
        await webActions.typeField('#dob-year', '2004');
        await webActions.typeField('#jointPartyIdentity_nino', 'SK112233A');
        await webActions.clickButton('Continue');
        await webActions.clickElementById('#jointPartyAddressSameAsAppellant_Yes');
        await webActions.clickButton('Continue');
    }

    async continueSubmission(): Promise<void> {
        await webActions.clickButton('Continue');
    }

    async delay(ms: number) {
        return new Promise( resolve => setTimeout(resolve, ms) );
    }

}

import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';
import issueDirectionsNoticeData from "./content/issue.direction_en.json";

let webActions: WebAction;

export class IssueDirectionPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('.govuk-caption-l', issueDirectionsNoticeData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', issueDirectionsNoticeData.eventNameHeading);
        await webActions.verifyPageLabel('[for=\'prePostHearing\'] > .form-label', issueDirectionsNoticeData.prepostHearingLabel);
        await webActions.verifyPageLabel('[for=\'directionTypeDl\'] > .form-label', issueDirectionsNoticeData.directionTypeLabel);
        await webActions.verifyPageLabel('[for=\'confidentialityType-general\']', issueDirectionsNoticeData.sendToAllPartiesLabel);
        await webActions.verifyPageLabel('[for=\'confidentialityType-confidential\']', issueDirectionsNoticeData.selectSpecificRecipientsLabel);
        await webActions.verifyPageLabel('#directionDueDate legend > .form-label', issueDirectionsNoticeData.dueDateLabel);
        await webActions.verifyPageLabel('[for=\'directionDueDate-day\']', issueDirectionsNoticeData.dayLabel);
        await webActions.verifyPageLabel('[for=\'directionDueDate-month\']', issueDirectionsNoticeData.monthLabel);
        await webActions.verifyPageLabel('[for=\'directionDueDate-year\']', issueDirectionsNoticeData.yearLabel);
        await webActions.verifyPageLabel('.form-label[_ngcontent-ng-c823086951]', issueDirectionsNoticeData.reservedToInterlocLabel);
        await webActions.verifyPageLabel('#generateNotice legend > .form-label', issueDirectionsNoticeData.generateNoticeLabel);
        await webActions.verifyPageLabel('[for=\'generateNotice_Yes\']', issueDirectionsNoticeData.yesNoticeLabel);
        await webActions.verifyPageLabel('[for=\'generateNotice_No\']', issueDirectionsNoticeData.noNoticeLabel);

    }

    async selectDirectionType(optionVal: string) {
        await webActions.chooseOptionByLabel('#directionTypeDl', optionVal);
    }

    async selectHearingOption(optionVal: string) {
        await webActions.chooseOptionByLabel('#prePostHearing', optionVal);
    }

    async chooseRecipients(optionVal: string) {
        await webActions.clickElementById(optionVal);
    }

    async verifySpecificRecipients() {
        await webActions.verifyPageLabel('#sendDirectionNoticeToFTA legend > .form-label', issueDirectionsNoticeData.ftaLabel);
        await webActions.verifyPageLabel('#sendDirectionNoticeToRepresentative legend > .form-label', issueDirectionsNoticeData.representativeLabel);
        await webActions.verifyPageLabel('#sendDirectionNoticeToOtherParty legend > .form-label', issueDirectionsNoticeData.otherPartyLabel);
        await webActions.verifyPageLabel('#sendDirectionNoticeToAppellantOrAppointee legend > .form-label', issueDirectionsNoticeData.appellantOrAppointee);
    }

    async populateSpecificRecipients() {
        await webActions.clickElementById("#sendDirectionNoticeToFTA_Yes");
        await webActions.clickElementById("#sendDirectionNoticeToRepresentative_No");
        // await webActions.clickElementById("#sendDirectionNoticeToOtherParty_Yes");
        await webActions.clickElementById("#sendDirectionNoticeToAppellantOrAppointee_No");
    }

    async enterDirectionDueDate() {
        await webActions.inputField('#directionDueDate-day', '21');
        await webActions.inputField('#directionDueDate-month', '10');
        await webActions.inputField('#directionDueDate-year', '2025');
    }

    async chooseNoticeType(optionVal: string) {
        await webActions.clickElementById(optionVal);
    }

    async enterNoticeContent(generateNoticeFlag) {
        if (!generateNoticeFlag) {
            await webActions.chooseOptionByLabel('#sscsInterlocDirectionDocument_documentType', 'Directions Notice');
            await webActions.uploadFileUsingAFileChooser('#sscsInterlocDirectionDocument_documentLink', 'testfile1.pdf');
            await webActions.delay(3000);
            await webActions.inputField('#documentDateAdded-day', '01');
            await webActions.inputField('#documentDateAdded-month', '01');
            await webActions.inputField('#documentDateAdded-year', '2024');
            await webActions.inputField('#sscsInterlocDirectionDocument_documentFileName','testfile1.pdf');
        } else {
            await webActions.inputField('#bodyContent', 'Test body content');
            await webActions.inputField('#signedBy', 'Tester');
            await webActions.inputField('#signedRole', 'Test');
        }
    }

    async submitContinueBtn(): Promise<void> {
        await webActions.clickButton("Continue");
    }

    async verifyDocumentTitle(expText: string) {
        await webActions.verifyTextVisibility(expText);
    }

    async confirmSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await webActions.clickSubmitButton();
        await this.page.waitForTimeout(3000);
    }

    async verifyErrorMsg(pageLevelErrorMessages, specificRecipientsErrorMessages, generateNoticeYesErrorMessages): Promise<void>{

        //await webActions.verifyElementVisibility('#errors');
        if (pageLevelErrorMessages) {
            await webActions.verifyTextVisibility('Pre or post hearing? is required');
            await webActions.verifyTextVisibility('Field is required');
            await webActions.verifyTextVisibility('Generate notice is required');
        }
        if (specificRecipientsErrorMessages === true) {
            await webActions.verifyTextVisibility('Pre or post hearing? is required');
            await webActions.verifyTextVisibility('FTA is required');
            await webActions.verifyTextVisibility('Representative is required');
            await webActions.verifyTextVisibility('Appellant or Appointee is required');
        }

        if (generateNoticeYesErrorMessages === true) {
            await webActions.verifyTextVisibility('Pre or post hearing? is required');
            await webActions.verifyTextVisibility('FTA is required');
            await webActions.verifyTextVisibility('Representative is required');
            await webActions.verifyTextVisibility('Appellant or Appointee is required');
            await webActions.verifyTextVisibility('Body content is required');
            await webActions.verifyTextVisibility('Signed by is required');
            await webActions.verifyTextVisibility('Signed role is required');
        }
    }

    async clickAddNewButton(): Promise<void> {
        await webActions.clickButton('Add new');
    }

    async submitIssueDirection(hearingOption: string, directionType: string, docTitle: string): Promise<void> {
        await this.verifyPageContent();
        await this.selectHearingOption(hearingOption);
        await this.selectDirectionType(directionType);
        await this.chooseRecipients('#confidentialityType-general');
        await this.chooseNoticeType('#generateNotice_Yes');
        await this.enterDirectionDueDate();
        await this.enterNoticeContent(true);
        await this.confirmSubmission();
        await this.verifyDocumentTitle(docTitle);
        await this.confirmSubmission();
    }

    async populatePreHearingAppealToProceed(hearingOption: string, directionType: string, docTitle: string): Promise<void> {
        await this.verifyPageContent();
        await this.selectHearingOption(hearingOption);
        await this.selectDirectionType(directionType);
        await this.chooseRecipients('#confidentialityType-general');
        await this.chooseNoticeType('#generateNotice_Yes');
        await this.enterDirectionDueDate();
        await this.enterNoticeContent(true);
        await this.confirmSubmission();
        await this.verifyDocumentTitle(docTitle);
        await this.confirmSubmission();
    }

    async populatePostHearingESAAppealToProceed(hearingOption: string, directionType: string, docTitle: string): Promise<void> {
        await this.selectHearingOption(hearingOption);
        await this.selectDirectionType(directionType);
        await this.chooseRecipients('#confidentialityType-general');
        await this.chooseNoticeType('#generateNotice_Yes');
        await this.enterDirectionDueDate();
        await this.enterNoticeContent(true);
        await this.confirmSubmission();
        await this.verifyDocumentTitle(docTitle);
        await this.confirmSubmission();
    }

    async populatePostHearingDLAProvideInformation(hearingOption: string, directionType: string, docTitle: string): Promise<void> {
        await this.selectHearingOption(hearingOption);
        await this.selectDirectionType(directionType);
        await this.chooseRecipients('#confidentialityType-confidential');
        await this.verifySpecificRecipients();
        await this.populateSpecificRecipients();
        await this.chooseNoticeType('#generateNotice_No');
        await this.enterNoticeContent(false);
        await this.enterDirectionDueDate();
        await this.confirmSubmission();
    }
}

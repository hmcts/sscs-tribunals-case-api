import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';
import postponementData from "./content/postponement.details_en.json";

let webActions: WebAction;

export class PostponementPages {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContentPostponementDetailsPage() {
        await webActions.verifyPageLabel('.govuk-caption-l', postponementData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', postponementData.postponementDetailsPageHeading);

        await webActions.verifyPageLabel('[field_id=\'postponementRequestHearingDateAndTime\'] .case-field__label', postponementData.dateAndTimeLabel);
        await webActions.verifyPageLabel('[field_id=\'postponementRequestHearingVenue\'] .case-field__label', postponementData.venueLabel);
        await webActions.verifyPageLabel('.form-label', postponementData.postponementDetailsLabel);

    }

    async verifyPageContentActionPostponementRequestPage(actionPostponementRequest: string) {
        await webActions.verifyPageLabel('.govuk-caption-l', postponementData.actionPostponementRequestEventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', postponementData.actionPostponementRequestPageHeading);

        if (actionPostponementRequest === 'Send to Judge') {

        } else {
            await webActions.verifyPageLabel('[for=\'actionPostponementRequestSelected\'] > .form-label', postponementData.actionPostponementRequestFieldLabel);
            await webActions.verifyPageLabel('.inline > legend > .form-label', postponementData.tribunalDirectPOToAttendLabel);
            await webActions.verifyPageLabel('[for=\'tribunalDirectPoToAttend_Yes\']', postponementData.yesLabel);
            await webActions.verifyPageLabel('[for=\'tribunalDirectPoToAttend_No\']', postponementData.noLabel);
        }
    }

    async inputPostponementDetailsOfPageData() {
        await webActions.inputField('#postponementRequestDetails', postponementData.postponementDetailsInputValue);
    }

    async inputPostponementActionRequestPageData(actionPostponementRequest: string) {
        await webActions.chooseOptionByLabel('#actionPostponementRequestSelected', actionPostponementRequest);
        if (actionPostponementRequest === 'Grant Postponement') {
            await webActions.verifyPageLabel('[for=\'bodyContent\'] > .form-label', postponementData.bodyContentLabel);
            await webActions.verifyPageLabel('.form-label[_ngcontent-ng-c823086951]', postponementData.reservedToInterLocLabel);
            await webActions.verifyPageLabel('[for=\'signedBy\'] > .form-label', postponementData.signedByLabel);
            await webActions.verifyPageLabel('[for=\'signedRole\'] > .form-label', postponementData.signedRoleLabel);
            await webActions.verifyPageLabel('[for=\'listingOption\'] > .form-label', postponementData.listingOptionLabel);
            await webActions.inputField('#bodyContent', postponementData.bodyContentInputValue);
            await webActions.inputField('#reservedToJudgeInterloc', postponementData.reservedToJudgeInterlocInputValue);
            await webActions.inputField('#signedBy', postponementData.signedByInputValue);
            await webActions.inputField('#signedRole', postponementData.signedRoleInputValue);
            await webActions.chooseOptionByLabel('#listingOption', postponementData.readyToListInputValue);
        } else if (actionPostponementRequest === 'Refuse Postponement') {
            await webActions.verifyPageLabel('[for=\'bodyContent\'] > .form-label', postponementData.bodyContentLabel);
            await webActions.verifyPageLabel('.form-label[_ngcontent-ng-c823086951]', postponementData.reservedToInterLocLabel);
            await webActions.verifyPageLabel('[for=\'signedBy\'] > .form-label', postponementData.signedByLabel);
            await webActions.verifyPageLabel('[for=\'signedRole\'] > .form-label', postponementData.signedRoleLabel);
            await webActions.verifyPageLabel('[for=\'listingOption\'] > .form-label', postponementData.listingOptionLabel);
            await webActions.inputField('#bodyContent', postponementData.bodyContentInputValue);
            await webActions.inputField('#reservedToJudgeInterloc', postponementData.reservedToJudgeInterlocInputValue);
            await webActions.inputField('#signedBy', postponementData.signedByInputValue);
            await webActions.inputField('#signedRole', postponementData.signedRoleInputValue);
        }
        else if (actionPostponementRequest === 'Send to Judge') {
            await webActions.verifyPageLabel('[for=\'postponementRequestDetails\'] > .form-label', postponementData.postponementDetailsLabel);
            await webActions.verifyPageLabel('.form-label[_ngcontent-ng-c823086951]', postponementData.reservedToInterLocLabel);
            await webActions.inputField('#postponementRequestDetails', postponementData.bodyContentInputValue);
            await webActions.inputField('#reservedToJudgeInterloc', postponementData.reservedToJudgeInterlocInputValue);
        }
        await webActions.verifyPageLabel('.inline > legend > .form-label', postponementData.tribunalDirectPOToAttendLabel);
        await webActions.clickElementById('#tribunalDirectPoToAttend_Yes');
    }

    async verifyPageContentPreviewPostponementDocumentPage() {
        await webActions.verifyPageLabel('.govuk-caption-l', postponementData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', postponementData.previewPostponementRequestDocumentPageHeading);

        await webActions.verifyPageLabel('.form-label', postponementData.postponementPreviewLabel);
        await webActions.verifyPageLabel('.form-hint', postponementData.postponementPreviewGuidanceText);
    }

    async verifyPageContentActionPostponementRequestDocumentPage() {
        await webActions.verifyPageLabel('.govuk-caption-l', postponementData.actionPostponementRequestEventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', postponementData.actionPostponementRequestPageHeading);
        //await webActions.verifyPageLabel('.form-label', postponementData.previewDocumentLabel);
    }

    async submitContinueBtn(): Promise<void> {
        await webActions.clickButton("Continue");
    }

    async submitBtn(): Promise<void> {
        await webActions.clickSubmitButton();
    }

    async confirmSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await webActions.clickSubmitButton();
        await this.page.waitForTimeout(3000);
    }
}

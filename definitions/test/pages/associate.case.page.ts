import { expect, Locator, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
const associateCaseTestData = require("../pages/content/associate.case_en.json");

let webAction: WebAction;

export class AssociateCasePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent(casereference : string) {
        await webAction.verifyPageLabel('.govuk-caption-l', associateCaseTestData.associateCaseCaption); //Captor Text
        await webAction.verifyPageLabel('.govuk-heading-l', associateCaseTestData.associateCaseHeading); //Heading Text
        await webAction.verifyPageLabel('#linkedCasesBoolean legend span', associateCaseTestData.associateCaseRelatedAppealsLabel); //Field Label
        await webAction.verifyPageLabel('label[for="linkedCasesBoolean_Yes"]', associateCaseTestData.associateCaseRelatedAppealsYesLabel);
        await webAction.verifyPageLabel('label[for="linkedCasesBoolean_No"]', associateCaseTestData.associateCaseRelatedAppealsNoLabel);
    }

    async associateCase(caseNumber: string): Promise<void> {
        await webAction.clickButton("Add new");
        await webAction.inputField('input#associatedCase_0_0', caseNumber);
        await webAction.clickRadioButton('No');
        await webAction.clickButton("Continue");
    }

    async associateNonExistentCase(caseNumber: string): Promise<void> {
        await webAction.clickButton("Add new");
        await webAction.inputField('input#associatedCase_0_0', caseNumber);
        await webAction.clickRadioButton('#linkedCasesBoolean_No');
        await webAction.clickButton("Continue");
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    }

    async verifyInputErrorMessage(caseNumber: string) {
        let errorMessageText = (await this.page.locator("div.form-group span.error-message").textContent()).trim();
        expect(errorMessageText).toEqual(`${caseNumber} does not correspond to an existing CCD case.`)
    }

    async cancelEvent(): Promise<void> {
        await webAction.clickLink("Cancel");
    }
 
}
import { Page, expect } from '@playwright/test';
import { WebAction } from '../common/web.action';
import requestInfoData from "./content/request.info.from.party_en.json";
import dateUtilsComponent from '../utils/DateUtilsComponent';

let webAction: WebAction;

export class RequestInfoFromPartyPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('span.govuk-caption-l', requestInfoData.requestInfoCaption); //Caption text
        await webAction.verifyPageLabel('h1.govuk-heading-l', requestInfoData.requestInfoHeading); //Heading text
        await webAction.verifyPageLabel('div#informationFromAppellant fieldset legend', requestInfoData.requestInfoFromCasePartyFieldSetLabel); //Fieldset Label
        await webAction.verifyPageLabel('label[for=\'informationFromAppellant_Yes\']', requestInfoData.requestInfoFromCasePartyYesRadioButtonLabel);
        await webAction.verifyPageLabel('label[for=\'informationFromAppellant_Yes\']', requestInfoData.requestInfoFromCasePartyYesRadioButtonLabel);
        await webAction.verifyPageLabel('div#responseRequired fieldset legend', requestInfoData.requestInfoResponseRequiredFieldSetLabel); //Fieldset Label
        await webAction.verifyPageLabel('label[for=\'responseRequired_Yes\']', requestInfoData.requestInfoResponseRequiredYesRadioButtonLabel);
        await webAction.verifyPageLabel('label[for=\'responseRequired_No\']', requestInfoData.requestInfoResponseRequiredNoRadioButtonLabel);
    }

    async selectPartyToRequestInfoFrom(option: string): Promise<void> {
        let selector = '#informationFromPartySelected';
        await expect(this.page.locator(selector)).toBeVisible();
        await webAction.chooseOptionByLabel(selector, option);
    }

    async chooseRequestInfoFromCaseParty(): Promise<void> {
        await webAction.clickElementById('label[for=\'informationFromAppellant_Yes\']');
    }

    async chooseResponseRequired(): Promise<void> {
        await webAction.clickElementById('label[for=\'responseRequired_Yes\']');
    }

    async inputRequestDetails(requestDetails: string): Promise<void> {
        let selector = '#infoRequests_appellantInfoRequestCollection_0_appellantInfoParagraph';
        await expect(this.page.locator(selector)).toBeVisible();
        await webAction.inputField(selector, requestDetails);
    }

    async inputDateOfRequest() {
        let date = new Date();
        await this.page.locator('#appellantInfoRequestDate-day').fill(String(date.getDate()).padStart(2, '0'));
        await this.page.locator('#appellantInfoRequestDate-month').fill(String(date.getMonth() + 1).padStart(2, '0'));
        await this.page.locator('#appellantInfoRequestDate-year').pressSequentially(String(date.getFullYear()));
        await expect(this.page.locator('fieldset span.error-message')).toBeHidden();
    }

    async inputDueDate() {
        await expect(this.page.locator('#directionDueDate-day')).toBeVisible();
        let date = dateUtilsComponent.rollDateToCertainWeeks(1);
        await this.page.locator('#directionDueDate-day').fill(String(date.getDate()).padStart(2, '0'));
        await this.page.locator('#directionDueDate-month').fill(String(date.getMonth() + 1).padStart(2, '0'));
        await this.page.locator('#directionDueDate-year').pressSequentially(String(date.getFullYear()));
        await expect(this.page.locator('fieldset span.error-message')).toBeHidden();
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    } 
}
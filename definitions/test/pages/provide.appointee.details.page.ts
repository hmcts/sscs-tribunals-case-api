import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';


let webAction: WebAction;

export class ProvideAppointeeDetailsPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent(heading: string) {
        await webAction.verifyPageLabel('h1.govuk-heading-l', heading); //Page heading
        await webAction.isLinkClickable('Cancel');
    }

    async verifyDropDownElementNotVisble(labelText: string): Promise<void> {
        await this.page.locator('#next-step').click();

        // Check if the labelText is visible in the expanded dropdown
        const isLabelVisible = await this.page.locator(`text=${labelText}`).isVisible();
        
        if (isLabelVisible) {
            throw new Error(`Label text "${labelText}" is visible in the dropdown options.`);
        }
    }

    async verifyAndPopulateAppointeeDetailsPage(appointeeData){
        await webAction.verifyPageLabel('//h2[.=\'Appointee details\']', appointeeData.appointeeDetailsSectionHeading);
        await webAction.verifyPageLabel('//h2[.=\'Identity\']', appointeeData.identitySectionHeading);
        await webAction.verifyPageLabel('//h2[.=\'Address Details\']', appointeeData.addressDetailsSectionHeading);
        await webAction.verifyPageLabel('//h2[.=\'Contact Details\']', appointeeData.contactDetailsSectionHeading);

        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_name_title\'] > .form-label', appointeeData.titleTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_name_firstName\'] > .form-label', appointeeData.firstNameTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_name_lastName\'] > .form-label', appointeeData.lastNameTextFieldLabel);
        await webAction.verifyPageLabel('#appeal_appellant_appointee_identity_identity legend > .form-label', appointeeData.dateOfBirthTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_identity_nino\'] > .form-label', appointeeData.nationalInsuranceNumberTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_address_line1\'] > .form-label', appointeeData.addressLine1TextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_address_line2\'] > .form-label', appointeeData.addressLine2TextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_address_town\'] > .form-label', appointeeData.townTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_address_county\'] > .form-label', appointeeData.countyTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_address_postcode\'] > .form-label', appointeeData.postcodeTextFieldLabel);

        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_contact_phone\'] > .form-label', appointeeData.contactNumberTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_contact_mobile\'] > .form-label', appointeeData.mobileNumberTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'appeal_appellant_appointee_contact_email\'] > .form-label', appointeeData.contactEmailTextFieldLabel);

        await webAction.inputField('#appeal_appellant_appointee_name_title', appointeeData.nameTitleValue);
        await webAction.inputField('#appeal_appellant_appointee_name_firstName', appointeeData.firstNameValue);
        await webAction.inputField('#appeal_appellant_appointee_name_lastName', appointeeData.lastNameValue);
        await webAction.typeField('#appeal_appellant_appointee_identity_identity #dob-day', appointeeData.dobDayValue);
        await webAction.typeField('#appeal_appellant_appointee_identity_identity #dob-month', appointeeData.dobMonthValue);
        await webAction.typeField('#appeal_appellant_appointee_identity_identity #dob-year', appointeeData.dobYearValue);
        await webAction.inputField('#appeal_appellant_appointee_identity_nino', appointeeData.ninoValue);

        await webAction.inputField('#appeal_appellant_appointee_address_address ccd-field-write:nth-of-type(1) .form-control', appointeeData.streetAddressValue1);
        await webAction.inputField('#appeal_appellant_appointee_address_address ccd-field-write:nth-of-type(2) .form-control', appointeeData.streetAddressValue2);
        await webAction.inputField("#appeal_appellant_appointee_address_town", appointeeData.townValue);
        await webAction.inputField('#appeal_appellant_appointee_address_county', appointeeData.countyValue);
        await webAction.inputField('#appeal_appellant_appointee_address_postcode', appointeeData.postcodeValue);
        await webAction.inputField('#appeal_appellant_appointee_contact_phone', appointeeData.phoneValue);
        await webAction.inputField('#appeal_appellant_appointee_contact_mobile', appointeeData.mobileValue); 
        await webAction.inputField('#appeal_appellant_appointee_contact_email', appointeeData.emailValue);
    }

    async chooseAssistOption(optionVal: string): Promise<void> {
        await webAction.clickElementById(`#appeal_appellant_isAppointee_${optionVal}`);
    }

    async continueSubmission(): Promise<void> {
        await webAction.clickButton('Continue');
    }

}

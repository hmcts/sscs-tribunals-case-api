import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';
import {ProvideAppointeeDetailsPage} from './provide.appointee.details.page';
import deathOfAnAppellant from "./content/death.of.an.appellant_en.json";
import appointeeDetails from "./content/appointee.details_en.json";

let webAction: WebAction;

export class DeathOfAppellantPage {

    readonly page: Page;
    protected provideAppointeeDetails: ProvideAppointeeDetailsPage;

    constructor(page: Page) {
        this.page = page;
        this.provideAppointeeDetails = new ProvideAppointeeDetailsPage(this.page);
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('.govuk-heading-l', deathOfAnAppellant.deathOfAppellantHeading); //Heading Text
        await webAction.verifyPageLabel('//span[.=\'Date of appellant death\']', deathOfAnAppellant.dateOfAppellantDeathTextFieldLabel); //Field Label
        await webAction.verifyPageLabel('[for=\'dateOfAppellantDeath-day\']', deathOfAnAppellant.dayAppellantDeathTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'dateOfAppellantDeath-month\']', deathOfAnAppellant.monthAppellantDeathTextFieldLabel);
        await webAction.verifyPageLabel('[for=\'dateOfAppellantDeath-year\']', deathOfAnAppellant.yearAppellantDeathTextFieldLabel);
        await webAction.verifyPageLabel('//h2[.=\'Appeal\']', deathOfAnAppellant.appealSectionHeading);
        await webAction.verifyPageLabel('//h2[.=\'Appellant Details\']', deathOfAnAppellant.appellantDetailsSectionHeading);
    }

    async populateDeathOfAppellantPageData(yesNoOption: string) {

        //The Appointee is selected first as there is an issue with the date.
        await webAction.clickElementById(`#appeal_appellant_isAppointee_${yesNoOption}`);
        await webAction.typeField('#dateOfAppellantDeath-day', '01');
        await webAction.typeField('#dateOfAppellantDeath-month', '06');
        await webAction.typeField('#dateOfAppellantDeath-year', '2003');

        if (yesNoOption === 'Yes') {

            //Verify Section Headings and Field Labels for the Appointee Details (Name, Identify, Address Details, Contact Details)
            await webAction.verifyPageLabel('//h2[.=\'Appointee details\']', deathOfAnAppellant.appointeeDetailsSectionHeading);
            await webAction.verifyPageLabel('//h2[.=\'Identity\']', deathOfAnAppellant.identitySectionHeading);
            await webAction.verifyPageLabel('//h2[.=\'Address Details\']', deathOfAnAppellant.addressDetailsSectionHeading);
            await webAction.verifyPageLabel('//h2[.=\'Contact Details\']', deathOfAnAppellant.contactDetailsSectionHeading);

            this.provideAppointeeDetails.verifyAndPopulateAppointeeDetailsPage(appointeeDetails);
        }
    }

    async populateDeathOfAppellantDateInvalidFormat(yesNoOption: string) {
        await webAction.clickElementById(`#appeal_appellant_isAppointee_${yesNoOption}`);
        await webAction.typeField('#dateOfAppellantDeath-day', '01');
        await webAction.typeField('#dateOfAppellantDeath-month', 'AUG');
        await webAction.typeField('#dateOfAppellantDeath-year', '2028');
    }

    async populateDeathOfAppellantDateInTheFuture(yesNoOption: string) {
        await webAction.clickElementById(`#appeal_appellant_isAppointee_${yesNoOption}`);
        await webAction.typeField('#dateOfAppellantDeath-day', '01');
        await webAction.typeField('#dateOfAppellantDeath-month', '08');
        await webAction.typeField('#dateOfAppellantDeath-year', '2028');
    }

    async confirmSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await webAction.clickSubmitButton();
    }

    async signOut(): Promise<void> {
        await webAction.clickElementById("//a[contains(.,'Sign out')]");
    }

    async verifyDeathDateNotBeIntheFutureErrorMsg(): Promise<void> {
        await webAction.verifyTextVisibility('Date of appellant death must not be in the future');
    }

    async verifyDeathDateNotValidErrorMsg(): Promise<void> {
        await webAction.verifyTextVisibility('Date of appellant death is not valid');
    }

    async reloadPage() {
        await this.page.reload({timeout: 13000, waitUntil: 'load'});
    }
}

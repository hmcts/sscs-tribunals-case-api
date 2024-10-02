import {expect, Page} from '@playwright/test';
import {WebAction} from '../../common/web.action';
import dateUtilsComponent from '../../utils/DateUtilsComponent';


let webActions: WebAction;

export class Hearings {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../div/div[normalize-space()="${fieldValue}"]`).first()).toBeVisible();
    }

    async verifyVenueListForPaperCase(fieldLabel: string, venueTotal: number) {
        await webActions.verifyTotalElements(`//*[normalize-space()='${fieldLabel}']/../div/div/ul/li`, venueTotal);
    }

    async verifyHearingStatusSummary() {
        await webActions.verifyTextVisibility("WAITING TO BE LISTED");
        await webActions.verifyTextVisibility("Substantive");
        await webActions.verifyTextVisibility("View or edit");
        await webActions.isLinkClickable("Cancel");
    }

    async clickHearingDetails() {
        await webActions.clickLink('View or edit');
    }

    async clickBackLink() {
        await webActions.clickElementById('a.govuk-back-link');
    }

    async verifyExpHearingDateIsGenerated(noOfDays: string) {
        const hearingBookedDate = new Date();
        const numberOfDaysToAdd = Number(noOfDays);

        hearingBookedDate.setDate(new Date().getDate() + numberOfDaysToAdd);
        let formattedDate = dateUtilsComponent.formatDateToSpecifiedDateFormat(hearingBookedDate);
        await webActions.verifyTextVisibility(formattedDate);
    }

    async clickCancelLink() {
        await webActions.verifyTextVisibility('Cancel');
        await webActions.clickLink('Cancel');
    }

    async submitCancellationReason() {
        await webActions.clickElementById('#incompl');
        await webActions.clickButton('Continue');
    }

    async verifyCancellationStatusSummary() {
        await webActions.verifyTextVisibility("CANCELLATION REQUESTED");
        await webActions.verifyTextVisibility("Substantive");
        await webActions.verifyTextVisibility("View details");
    }

    async verifyCancellationDetails(cancelReason: string) {
        await webActions.clickLink('View details');
        await webActions.verifyTextVisibility('Cancellation requested');
        await webActions.verifyTextVisibility(cancelReason);
    }

    async updateHearingDuration() {
        await webActions.clickElementById('#hearingLength');
        await webActions.clearInputField('#durationhours');
        await webActions.typeField('#durationhours', '2');

        //To always choose weekdays for hearing dates
        const today = new Date();
        const resultDate = dateUtilsComponent.addWeekdays(today, 21);
        
        await webActions.clearInputField('#earliestHearingDate');
        await webActions.typeField('#earliestHearingDate', `${resultDate.getDate()}`);

        await webActions.clickButton('Continue');
    }

    async sumitUpdate() {
        await webActions.clickButton('Submit updated request');
        await webActions.clickElementById('#adminreq');
        await webActions.clickButton('Submit change request');
        await webActions.verifyTextVisibility('Hearing request submitted');
        await webActions.clickElementById('div.govuk-body a.govuk-link');
    }

    async verifyUpdateStatusSummary() {
        await webActions.verifyTextVisibility("UPDATE REQUESTED");
        await webActions.verifyTextVisibility("Substantive");
        await webActions.verifyTextVisibility("View or edit");
        await webActions.isLinkClickable("Cancel");
    }
 }

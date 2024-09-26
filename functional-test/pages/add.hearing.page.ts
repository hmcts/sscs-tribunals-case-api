import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
// import { faker } from '@faker-js/faker';

const faker = require('@faker-js/faker');

let webActions: WebAction;

export class AddHearingPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async submitHearing(hearingState : string = 'Hearing is Completed'): Promise<void> {
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Add hearing details');
        await webActions.clickButton('Add new');
        await webActions.inputField('#hearings_0_venue_name', 'Fox court');
        await webActions.inputField('#hearings_0_venue_address_line1', '20, test');
        await webActions.inputField('#hearings_0_venue_address_line2', 'test');
        await webActions.inputField('#hearings_0_venue_address_line3', 'test');
        await webActions.inputField('#hearings_0_venue_address_town', 'test'); 
        await webActions.inputField('#hearings_0_venue_address_county', 'test county');
        await webActions.inputField('#hearings_0_venue_address_postcode', 'TS3 3ST'); 
        await webActions.inputField('#hearings_0_venue_address_country', 'UK');
        await webActions.inputField('#hearings_0_venue_googleMapLink', 'Test');
        await webActions.clickElementById('#hearings_0_adjourned_Yes');

        await webActions.inputField('#hearingDate-day', '20');
        await webActions.inputField('#hearingDate-month', '2');
        await webActions.inputField('#hearingDate-year', '2024');
        await webActions.inputField('#hearings_0_time', '13:00');

        await webActions.inputField('#postponedDate-day', '20');
        await webActions.inputField('#postponedDate-month', '2');
        await webActions.inputField('#postponedDate-year', '2024');

        await webActions.inputField('#adjournedDate-day', '20');
        await webActions.inputField('#adjournedDate-month', '2');
        await webActions.inputField('#adjournedDate-year', '2024');

        await webActions.inputField('#hearings_0_hearingId', '1234');

        await webActions.inputField('#eventDate-day', '20');
        await webActions.inputField('#eventDate-month', '2');
        await webActions.inputField('#eventDate-year', '2024');

        
        await webActions.inputField('#hearings_0_venueId', '123');

        await webActions.inputField('#hearingRequested-day', '20');
        await webActions.inputField('#hearingRequested-month', '2');
        await webActions.inputField('#hearingRequested-year', '2024');

        
        await webActions.inputField('#hearings_0_versionNumber', '123');
        await webActions.chooseOptionByLabel('#hearings_0_hearingStatus', hearingState);

        await webActions.inputField('#start-day', '20');
        await webActions.inputField('#start-month', '2');
        await webActions.inputField('#start-year', '2024');

        await webActions.inputField('#end-day', '20');
        await webActions.inputField('#end-month', '2');
        await webActions.inputField('#end-year', '2024');

        
        await webActions.inputField('#hearings_0_epimsId', '1234');
        await webActions.chooseOptionByLabel('#hearings_0_hearingChannel', 'Face To Face');

        await webActions.clickSubmitButton();


    }
}

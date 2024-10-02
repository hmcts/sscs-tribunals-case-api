import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';

let webActions: WebAction;

export class HearingBookedPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async submitHearingBooked(): Promise<void> {
        await webActions.delay(5000);
        await webActions.verifyPageLabel('h1.govuk-heading-l', 'Hearing booked');
        await webActions.clickButton('Submit');
    }
}

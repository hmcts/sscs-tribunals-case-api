import {Page} from '@playwright/test';
import {WebAction} from '../../common/web.action'
import {isUndefined} from "node:util";
const eventTestData = require("../content/event.name.event.description_en.json");

let webActions: WebAction;

export class EventNameEventDescriptionPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent(headingValue: string, checkYourAnswersFlag: boolean = false, key?: string, value?: string) {

        await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
        if (checkYourAnswersFlag) {
            await webActions.verifyPageLabel('.heading-h2', eventTestData.eventSummarycheckYourAnswersHeading); //Check your answers Text.
            await webActions.verifyPageLabel('.check-your-answers h2.heading-h2 + span', eventTestData.eventSummaryCheckTheInformationText);
            await webActions.verifyPageLabel('.case-field-label > .text-16', key);
            if (typeof (value) === 'undefined') {
            } else {
                await webActions.verifyPageLabel('ccd-read-text-area-field > span', value);
            }
        }
        await webActions.verifyPageLabel('[for=\'field-trigger-summary\']', eventTestData.eventSummaryLabel); //Field Label
        //await webActions.verifyPageLabel('.form-hint', eventTestData["event-summary-guidance-text"]); //Guidance Text
        await webActions.verifyPageLabel('[for=\'field-trigger-description\']', eventTestData.eventSummaryDescription); //Field Label
    }

    async inputData(eventSummary: string, eventDescription: string): Promise<void> {
        await webActions.inputField('#field-trigger-summary', eventSummary);
        await webActions.inputField('#field-trigger-description', eventDescription);
    }

    async confirmSubmission(): Promise<void> {
        await this.delay(3000);
        await webActions.clickSubmitButton();
        await this.delay(3000);
    }

    async submitBtn(): Promise<void> {
        await webActions.clickButton("Submit");
    }

    async delay(ms: number) {
        return new Promise( resolve => setTimeout(resolve, ms) );
    }
}

import { Page } from '@playwright/test';
import informationReceivedData from "../../pages/content/information.received_en.json";
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import { BaseStep } from './base';

export class InformationReceived extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performInformationReceivedEvent() {

        await this.informationReceivedPage.verifyPageContent();
        await this.informationReceivedPage.selectReviewState(informationReceivedData.informationReceivedReviewStateSelectValue);
        await this.informationReceivedPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(informationReceivedData.informationReceivedCaption);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.verifyHistoryTabDetails('With FTA', 'Information received', eventTestData.eventDescriptionInput);
    }

}

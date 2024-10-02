import { Page } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
import { SendCaseToTcwPage } from '../../pages/send.case.to.tcw.page';
import sendCaseToTcwData from "../../pages/content/send.case.to.tcw_en.json";
import eventTestData from "../../pages/content/event.name.event.description_en.json";

export class SendCaseToTcw extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performSendCaseToTcw(caseId: string) {

        await this.loginUserWithCaseId(credentials.judge, true, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Send case to TCW');

        let sendCaseToTcwPage = new SendCaseToTcwPage(this.page);
        await sendCaseToTcwPage.verifyPageContent();
        await sendCaseToTcwPage.enterNote(sendCaseToTcwData.enterNoteInput);
        await sendCaseToTcwPage.selectInterlocutoryReviewState(sendCaseToTcwData.interlocReviewStateSelectValue);
        await sendCaseToTcwPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent('Send case to TCW');
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.verifyHistoryTabDetails('With FTA', 'Send case to TCW', eventTestData.eventDescriptionInput);
    }
}

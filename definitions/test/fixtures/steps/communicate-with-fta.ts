import { Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';

export interface EventDetails {
    dateOfEvent: string;
    authorOfEvent: string;
}

export class CommunicateWithFta extends BaseStep {
    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async communicateWithFta(caseId: string) {
        await this.loginUserWithCaseId(credentials.caseWorker, false, caseId);
        await this.homePage.chooseEvent('Communication with FTA');
    }

    async submitNewCommunicationRequesttoFta(): Promise<EventDetails> {
        await this.communicateWithFtaPage.verifyPageContent();
        await this.communicateWithFtaPage.selectCommunicationType('New Request');
        await this.communicateWithFtaPage.fillOutNewRequestData();
        await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');

        const dateOfEvent = await this.historyTab.getDateOfEvent();
        const authorOfEvent = await this.historyTab.getAuthorOfEvent();
        return await { dateOfEvent, authorOfEvent } as EventDetails
    }

    async replyToCaseWorkersQueryToFta(caseId: string) {
        await this.signOut();
        //change to fta user
        await this.loginUserWithCaseId(credentials.caseWorker, false, caseId);
        await this.homePage.chooseEvent('Communication with Tribunal');
        await this.communicateWithTribunalPage.replyToTribunalQuery();
        await this.verifyHistoryTabDetails('With FTA', 'Communication with Tribunal');
    }

    async reviewFtaReply(caseId: string) {
        await this.signOut();
        //change to caseworker user
        await this.loginUserWithCaseId(credentials.caseWorker, false, caseId);
        await this.homePage.chooseEvent('Communication with FTA');
        await this.communicateWithFtaPage.fillOutReviewFtaReply();
        await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');
    }

    // async verifyNotificationEventDetails(dateOfRequest: string, authorOfRequest: string, requestTopic: string) {
    //     // to allow time for the event to be logged
    //     await this.homePage.navigateToTab('Tribunal/FTA Communications');
    //     await this.tribunalFtaCommunicationsTab.verifyRequestFromTribunal(dateOfRequest, authorOfRequest, requestTopic);
    // }
}


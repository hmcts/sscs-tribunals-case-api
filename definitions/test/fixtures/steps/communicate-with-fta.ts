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
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, false, caseId);
        await this.homePage.chooseEvent('Communication with FTA');
    }

    async submitNewCommunicationRequesttoFta() {
        await this.communicateWithFtaPage.verifyPageContent();
        await this.communicateWithFtaPage.selectCommunicationType('New Request');
        await this.communicateWithFtaPage.fillOutNewRequestData();
        await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');
        await this.homePage.navigateToTab('Tribunal/FTA Communications');
        await this.tribunalFtaCommunicationsTab.verifyRequestFromTribunalExists();
    }

    async replyToCaseWorkersQueryToFta(caseId: string) {
        await this.signOut();
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.chooseEvent('Communication with Tribunal');
        await this.communicateWithTribunalPage.replyToTribunalQuery();
        await this.verifyHistoryTabDetails('With FTA', 'Communication with Tribunal');
        await this.homePage.navigateToTab('Tribunal/FTA Communications');
        await this.tribunalFtaCommunicationsTab.verifyReplyExists();
    }

    async reviewFtaReply(caseId: string) {
        await this.signOut();
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, false, caseId);
        await this.homePage.chooseEvent('Communication with FTA');
        await this.communicateWithFtaPage.fillOutReviewFtaReply();
        await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');
        await this.homePage.navigateToTab('Tribunal/FTA Communications');
        await this.tribunalFtaCommunicationsTab.verifyReplyHasBeenReviewed();
    }
}


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

    async communicateWithUser(caseId: string, event: string, user: string) {
        await this.loginUserWithCaseId(credentials[user], false, caseId);
        await this.homePage.chooseEvent(event);
    }

    async submitNewCommunicationRequest(options: {isCommsToFta: boolean }){
        if (options.isCommsToFta) {
            await this.communicateWithFtaPage.verifyPageContent(true);
            await this.communicateWithFtaPage.selectCommunicationType('New Request');
            await this.communicateWithFtaPage.fillOutNewRequestData('Appeal Type', 'FTA');
            await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');
            await this.homePage.navigateToTab('Tribunal/FTA Communications');
            await this.tribunalFtaCommunicationsTab.verifyRequestFromTribunalExists();
        } else {
            await this.communicateWithFtaPage.verifyPageContent(false);
            await this.communicateWithFtaPage.selectCommunicationType('New Request');
            await this.communicateWithFtaPage.fillOutNewRequestData('Appeal Type', 'Caseworker');
            await this.homePage.navigateToTab('Tribunal/FTA Communications');
            await this.tribunalFtaCommunicationsTab.verifyRequestFromFTAExists();
        }
    }

    async replyToQuery(caseId: string, user: string, options: { 
        event: 'Communication with Tribunal' | 'Communication with FTA', 
        replyMethod: 'replyToTribunalQuery' | 'replyToFTAQuery'
    }) {
        await this.signOut();
        await this.loginUserWithCaseId(credentials[user], false, caseId);
        await this.homePage.chooseEvent(options.event);
        await this.communicateWithTribunalPage[options.replyMethod]();
        await this.homePage.navigateToTab('Tribunal/FTA Communications');
        await this.tribunalFtaCommunicationsTab.verifyReplyExists();
    }

    async reviewUserReply(caseId: string, options: {
        tribsVerifyReply: boolean,
        userType: 'amTribunalCaseWorker' | 'dwpResponseWriter',
        event: 'Communication with Tribunal' | 'Communication with FTA',
        reviewMethod: 'fillOutReviewTribunalReply' | 'fillOutReviewFtaReply'
    }) {
         await this.signOut();
        //change to caseworker user
        await this.loginUserWithCaseId(credentials[options.userType], false, caseId);
        await this.homePage.chooseEvent(options.event);
        await this.communicateWithFtaPage[options.reviewMethod]();
        if(options.tribsVerifyReply) await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');
        await this.homePage.navigateToTab('Tribunal/FTA Communications');
        await this.tribunalFtaCommunicationsTab.verifyReplyHasBeenReviewed(options.tribsVerifyReply);
    }

    async deleteRequestOrReply() {
        await this.homePage.chooseEvent('Communication with FTA');
        await this.communicateWithFtaPage.verifyPageContent(true);
        await this.communicateWithFtaPage.deleteRequestOrReply();
        await this.homePage.navigateToTab('Notepad');
        await this.notePadTab.verifyRequestOrReplyDeleteNoteExists('Request deleted: Appeal Type', 'Test reason for deleting request/reply');
    }
}


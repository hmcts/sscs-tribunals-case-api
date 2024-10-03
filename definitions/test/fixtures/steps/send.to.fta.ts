import { Page } from '@playwright/test';
import { BaseStep } from './base';
import {credentials} from "../../config/config";

import eventTestData from "../../pages/content/event.name.event.description_en.json";
import {accessId, accessToken, getSSCSServiceToken} from "../../api/client/idam/idam.service";
import {performEventOnCaseWithUploadResponse} from "../../api/client/sscs/factory/appeal.update.factory";

export class SendToFTA extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performSendToFTA(caseId: string) {

        //Progress the Case to FTA Response
        let ftaResponseWriterBearerToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let ftaResponseWriter: string = await accessId(credentials.dwpResponseWriter);
        await new Promise(f => setTimeout(f, 3000)); //Delay required for the Case to be ready
        await performEventOnCaseWithUploadResponse(ftaResponseWriterBearerToken.trim(),
            serviceToken.trim(), ftaResponseWriter.trim(),
            'SSCS','Benefit',
            caseId.trim(),'dwpUploadResponse','dwp');

        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        //Now Perform the respective Event.
        await this.homePage.chooseEvent('Admin - send to With FTA');
        await this.eventNameAndDescriptionPage.verifyPageContent("Admin - send to With FTA");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails('With FTA', 'Admin - send to With FTA', 'Event Description for Automation Verification');
    }

}

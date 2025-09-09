import { Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';

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

    async submitCommunicationRequesttoFta(communicationType: string, requestTopic: string): Promise<string> {
        await this.communicateWithFtaPage.verifyPageContent();
        await this.communicateWithFtaPage.selectCommunicationType(communicationType);
        await this.communicateWithFtaPage.fillOutRequestData(requestTopic);
        await this.verifyHistoryTabDetails('With FTA', 'Communication with FTA');
        return await this.historyTab.getDateOfEvent();
    }

   
}
import { Locator, expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import { listenerCount } from 'process';
import { Browser } from 'puppeteer';
//import addUpdateSubscriptionData from "./content/update.subscription.data_en.json";


let webAction: WebAction;

export class SearchFilterPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }
    
    async performSearch() {
        await this.page.locator('#wb-jurisdiction').selectOption({ label: 'Tribunals' });
        await this.page.locator('#wb-case-type').selectOption({ label: 'SSCS Case 6.4.4-E2E AAT' });
        await this.page.locator('#wb-case-state').selectOption({ label: 'Appeal Created' });
        await this.page.locator('#benefitCode').selectOption({ label: '002' });
        await this.page.locator('#issueCode').selectOption({ label: 'DD' });
        await this.page.getByRole('button', { name: 'Apply' }).click();
        await this.page.waitForLoadState();
        await expect(this.page.locator('#search-result-heading__text')).toContainText('Your cases');
        await expect(this.page.locator('#search-result-summary__text')).toContainText('results');
    }

    async validateSearchResults(caseId: number) {
        const locator: Locator = this.page.locator('ccd - search - result: nth - child(1) > table: nth - child(1) > tbody: nth - child(3) > tr');
        const tot: number = await locator.count();
        expect(tot).toBe(caseId);
    }
}
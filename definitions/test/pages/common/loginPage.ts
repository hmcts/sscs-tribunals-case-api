import { expect, Locator, Page } from '@playwright/test';
import { credentials } from '../../config/config';
import { WebAction } from '../../common/web.action'

let webActions: WebAction;

export class LoginPage {

    readonly page: Page;
    readonly pageTitle: Locator;
    readonly mainPageTitle: Locator;

    constructor(page: Page) {
        this.page = page;
        this.pageTitle = page.locator('h3');
        this.mainPageTitle = page.locator('h1');
        webActions = new WebAction(this.page);
    }

    async delay(ms: number) {
        return new Promise( resolve => setTimeout(resolve, ms) );
    }

    async goToLoginPage(): Promise<void> {
        await this.page.goto("/");
    }

    async goToCase(caseId: string): Promise<void> {
        await this.page.goto(`/cases/case-details/${caseId}`);
    }

    async verifySuccessfulLoginForUser(user, clearCacheFlag?: boolean): Promise<void> {
        if(clearCacheFlag) await this.page.context().clearCookies();
        await webActions.inputField('#username', user.email);
        await webActions.inputField('#password', user.password);
        await webActions.clickButton('Sign in');
        await expect(this.page.locator('//li/a[normalize-space()=\'Sign out\']')).toBeVisible();
    }
}
import { expect, Locator, Page, test } from '@playwright/test';
import { urls } from '../../config/config';
import { WebAction } from '../../common/web.action';

let webActions: WebAction;

export class LoginPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly mainPageTitle: Locator;
  readonly signOutBtn: Locator;
  readonly crownCopyrightLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h3');
    this.mainPageTitle = page.locator('h1');
    this.signOutBtn = page.locator("//li/a[normalize-space()='Sign out']");
    this.crownCopyrightLink = page.locator('a', { hasText: '© Crown copyright' });
    webActions = new WebAction(this.page);
  }

  async delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async goToLoginPage(): Promise<void> {
    await this.page.goto('/');
  }

  async goToCase(caseId: string): Promise<void> {
    await this.page.goto(`/cases/case-details/SSCS/Benefit/${caseId}#Summary`);
  }

  async verifySuccessfulLoginForUser(
    user,
    clearCacheFlag?: boolean
  ): Promise<void> {
    if (clearCacheFlag) await this.page.context().clearCookies();

    const isConfidentiality = test.info().tags.includes('@confidentiality');
    const isLocalhost = this.page.url().includes('localhost');
    if (isConfidentiality && isLocalhost) {
      await webActions.inputField('[name="username"]', user.email);
      await webActions.clickButton('Sign in');
      const help = this.page.locator('a', { hasText: 'Get help' });
      await expect(help).toBeVisible();
      const acceptCookiesBtn = this.page.locator('button', { hasText: 'Accept analytics cookies' });
      if (await acceptCookiesBtn.isVisible()) {
        await acceptCookiesBtn.click();
      }
      await expect(help).toBeVisible();
      return;
    }

    await webActions.inputField('#username', user.email);
    await webActions.inputField('#password', user.password);
    await webActions.clickButton('Sign in');
    await webActions.delay(10000);
    await this.signOutBtn.isVisible();
  }
}

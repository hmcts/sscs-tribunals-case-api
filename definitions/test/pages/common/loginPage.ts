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

    const isLocalhost = this.page.url().includes('localhost');
    if (isLocalhost) {
        await this.localLogin(user);
        return;
    }

      const maxAttempts = 3;
      const usernameField = this.page.locator('#username');
      const passwordField = this.page.locator('#password');
      const signInButton = this.page.getByRole('button', {
          name: 'Sign in',
          exact: true
      });

      for (let attempt = 1; attempt <= maxAttempts; attempt++) {
          await webActions.inputField('#username', user.email);
          await webActions.inputField('#password', user.password);
          await webActions.clickButton('Sign in');

          const signedIn = await this.signOutBtn
              .isVisible({ timeout: 10000 })
              .catch(() => false);

          if (signedIn) {
              return;
          }

          const loginFormStillVisible =
              await usernameField.isVisible({ timeout: 2000 }).catch(() => false) &&
              await passwordField.isVisible({ timeout: 2000 }).catch(() => false) &&
              await signInButton.isVisible({ timeout: 2000 }).catch(() => false);

          if (!loginFormStillVisible) {
              break;
          }
      }

      await expect(this.signOutBtn).toBeVisible();
  }

    private async localLogin(user) {
        let loginAttempts = 0;
        const maxAttempts = 5;
        const help = this.page.locator('a', {hasText: 'Get help'});

        await webActions.inputField('[name="username"]', user.email);
        await webActions.clickButton('Sign in');

        while (loginAttempts < maxAttempts) {
            try {
                await expect(help).toBeVisible({timeout: 5000});
                break;
            } catch {
                const usernameField = this.page.locator('[name="username"]');
                if (await usernameField.isVisible({timeout: 2000})) {
                    loginAttempts++;
                    await webActions.inputField('[name="username"]', user.email);
                    await webActions.clickButton('Sign in');
                } else {
                    break;
                }
            }
        }
        const acceptCookiesBtn = this.page.locator('button', {hasText: 'Accept analytics cookies'});
        if (await acceptCookiesBtn.isVisible()) {
            await acceptCookiesBtn.click();
        }
        await expect(help).toBeVisible();
    }
}

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
  readonly oldUsernameField: Locator;
  readonly oldPasswordField: Locator;
  readonly signInButton: Locator;
  readonly newUsernameField: Locator;
  readonly newPasswordField: Locator;
  readonly continueButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h3');
    this.mainPageTitle = page.locator('h1');
    this.signOutBtn = page.locator("//li/a[normalize-space()='Sign out']");
    this.crownCopyrightLink = page.locator('a', { hasText: '© Crown copyright' });
    this.oldUsernameField = page.locator('#username');
    this.oldPasswordField = page.locator('#password');
    this.signInButton = page.getByRole('button', {name: 'Sign in',exact: true});
    this.newUsernameField = page.locator('#email');
    this.newPasswordField = page.locator('#password');
    this.continueButton = page.getByRole('button', {name: 'Continue', exact: true});
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
    user: { email: string; password?: string },
    clearCacheFlag?: boolean
  ): Promise<void> {
    if (clearCacheFlag) {
      await this.page.context().clearCookies();
    }

    const isLocalhost = this.page.url().includes('localhost');
    if (isLocalhost) {
      await this.localLogin(user);
      return;
    }

    let newLoginPresent = false;
    try {
      await expect(this.newUsernameField).toBeVisible({ timeout: 5000 });
      newLoginPresent = true;
    } catch {
      newLoginPresent = false;
    }

    console.log('verifySuccessfulLoginForUser: newLoginPresent=', newLoginPresent);
    
    await(newLoginPresent
      ? this.verifyNewSuccessfulLoginForUser(user)
      : this.verifyOldSuccessfulLoginForUser(user)
    );
  
    await expect(this.signOutBtn).toBeVisible({ timeout: 15000 });
  }

  async verifyOldSuccessfulLoginForUser(
    user: { email: string; password?: string }
  ): Promise<void> { 
      await this.oldUsernameField.fill(user.email);
      await this.oldPasswordField.fill(user.password ?? '');

      await expect(this.signInButton).toBeVisible();
      await expect(this.signInButton).toBeEnabled();

      await this.signInButton.click();

      // verifySuccessfulSignIn may return early on success
      await this.verifySuccessfulSignIn();
  }

  async verifyNewSuccessfulLoginForUser(
    user: { email: string; password?: string }
  ): Promise<void> {
      await this.newUsernameField.fill(user.email);

      // ensure the continue button is visible and clickable
      await expect(this.continueButton).toBeVisible();
      await expect(this.continueButton).toBeEnabled();
      await this.continueButton.click();

      // wait for password step to appear before filling
      await expect(this.newPasswordField).toBeVisible({ timeout: 5000 });
      await this.newPasswordField.fill(user.password ?? '');

      // re-use continue button for the second step (may be same locator)
      await expect(this.continueButton).toBeVisible();
      await expect(this.continueButton).toBeEnabled();
      await this.continueButton.click();

      // verifySuccessfulSignIn may return early on success
      await this.verifySuccessfulSignIn();
  }

  async verifySuccessfulSignIn(): Promise<void> {
    const signedIn = await this.signOutBtn
      .isVisible({ timeout: 15000 })
      .catch(() => false);
  }

  private async localLogin(user: { email: string; }) {
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

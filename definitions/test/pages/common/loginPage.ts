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
    this.oldUsernameField = this.page.locator('#username');
    this.oldPasswordField = this.page.locator('#password');
    this.signInButton = this.page.getByRole('button', {name: 'Sign in',exact: true});
    this.newUsernameField = this.page.locator('#email');
    this.newPasswordField = this.page.locator('#password');
    this.continueButton = this.page.getByRole('button', {name: 'Continue', exact: true});
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

    const maxAttempts = 3;
    // const newLoginPresent = await this.page
    //   .getByText('Enter your email address')
    //   .isVisible()
    //   .catch(() => false);

    // await(newLoginPresent
    //   ? this.verifyNewSuccessfulLoginForUser(maxAttempts, user)
    //   : this.verifyOldSuccessfulLoginForUser(maxAttempts, user)
    // );

    try {
      await webActions.verifyTextVisibility('Enter your email address');
      await this.verifyNewSuccessfulLoginForUser(maxAttempts, user)
    } catch {
      await this.verifyOldSuccessfulLoginForUser(maxAttempts, user)
    }
    
    await expect(this.signOutBtn).toBeVisible({ timeout: 15000 });
  }

  async verifyOldSuccessfulLoginForUser(
    maxAttempts: number,
    user: { email: string; password?: string }
  ): Promise<void> {
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      await this.oldUsernameField.fill(user.email);
      await this.oldPasswordField.fill(user.password ?? '');

      await expect(this.signInButton).toBeVisible();
      await expect(this.signInButton).toBeEnabled();

      await this.signInButton.click();

      // verifySuccessfulSignIn may return early on success
      await this.verifySuccessfulSignIn();
    }
  }

  async verifyNewSuccessfulLoginForUser(
    maxAttempts: number,
    user: { email: string; password?: string }
  ): Promise<void> {
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
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
  }

  async verifySuccessfulSignIn(): Promise<void> {
    const signedIn = await this.signOutBtn
      .isVisible({ timeout: 15000 })
      .catch(() => false);

    if (signedIn) {
      return;
    }

    const backAtLogin =
      (await this.oldUsernameField.isVisible({ timeout: 3000 }).catch(() => false)) &&
      (await this.oldPasswordField.isVisible({ timeout: 3000 }).catch(() => false));

    if (!backAtLogin) {
      // Not signed in and not back at login; exit to allow caller to handle accordingly
      return;
    }
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

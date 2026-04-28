import { Page, expect } from '@playwright/test';
import eventTestData from '../../pages/content/event.name.event.description_en.json';
import { BaseStep } from './base';
import { credentials } from '../../config/config';

export class ReadyToList extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async performReadyToListEvent(
    caseId: string,
    loginRequired: boolean = true,
    user = credentials.amCtscAdminNwLiverpool
  ): Promise<void> {
    if (loginRequired) {
      await this.loginUserWithCaseId(user, false, caseId);
    }
    await this.homePage.chooseEvent('Ready to list');
    await this.completeReadyToListEvent();
  }

  async completeReadyToListEvent(): Promise<void> {
    await this.eventNameAndDescriptionPage.verifyPageContent('Ready to list');
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.ignoreWarningsIfPresent();

    await expect(this.homePage.summaryTab).toBeVisible();
    await this.verifyHistoryTabDetails(
      'With FTA',
      'Ready to list',
      eventTestData.eventDescriptionInput
    );
    await this.waitForReadyToListState();
  }

  private async waitForReadyToListState(): Promise<void> {
    let lastError;

    for (let attempt = 1; attempt <= 10; attempt++) {
      try {
        await this.homePage.navigateToTab('Summary');
        await expect(
          this.page.locator(
            `//p[./strong[normalize-space()="Appeal status"] and contains(normalize-space(), "Ready to list")]`
          )
        ).toBeVisible();
        return;
      } catch (error) {
        lastError = error;
        if (attempt === 10) {
          throw lastError;
        }
        await this.homePage.delay(3000);
        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab).toBeVisible();
      }
    }
  }

  private async ignoreWarningsIfPresent(): Promise<void> {
    const ignoreWarningButton = this.page.getByRole('button', {
      name: 'Ignore Warning and Continue',
      exact: true
    });

    if (await ignoreWarningButton.isVisible().catch(() => false)) {
      await ignoreWarningButton.click();
    }
  }
}

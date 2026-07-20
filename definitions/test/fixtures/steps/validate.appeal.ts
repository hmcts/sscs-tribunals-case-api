import { Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';

export class ValidateAppeal extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  private getUser(role: 'caseworker' | 'superuser') {
    return role === 'superuser' ? credentials.superUser : credentials.amCaseWorker;
  }

  private async waitForAwaitOtherPartyData() {
    for (let attempt = 0; attempt < 8; attempt++) {
      await this.homePage.navigateToTab('Summary');
      const summaryState = await this.page
        .locator('#summaryState')
        .textContent()
        .catch(() => '');

      if (summaryState?.includes('Await Other Party Data')) {
        return;
      }

      await this.homePage.delay(5000);
      await this.homePage.reloadPage();
    }

    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPageSectionByKeyValue(
      'Appeal status',
      'Await Other Party Data'
    );
  }

  private async waitForRequestOtherPartyDataHistory() {
    for (let attempt = 0; attempt < 8; attempt++) {
      await this.homePage.navigateToTab('History');
      const historyEvent = await this.page
        .getByRole('link', { name: 'Request other party data' })
        .first()
        .isVisible()
        .catch(() => false);

      if (historyEvent) {
        await this.historyTab.verifyPageContentByKeyValue(
          'End state',
          'Await Other Party Data'
        );
        return;
      }

      await this.homePage.delay(5000);
      await this.homePage.reloadPage();
    }

    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyEventCompleted('Request other party data');
    await this.historyTab.verifyPageContentByKeyValue(
      'End state',
      'Await Other Party Data'
    );
  }

  async validateChildSupportIncompleteAppeal(
    caseId: string,
    role: 'caseworker' | 'superuser'
  ) {
    await this.loginUserWithCaseId(this.getUser(role), true, caseId);

    await this.homePage.chooseEvent('Update to case data');
    await this.page
      .getByRole('textbox', { name: 'FTA Issuing Office (Optional)' })
      .fill('Child Maintenance Service Group');
    await this.page
      .getByRole('group', { name: 'MRN/Review Decision Notice Date (Optional)' })
      .getByLabel('Day')
      .fill('01');
    await this.page
      .getByRole('group', { name: 'MRN/Review Decision Notice Date (Optional)' })
      .getByLabel('Month')
      .fill('01');
    await this.page
      .getByRole('group', { name: 'MRN/Review Decision Notice Date (Optional)' })
      .getByLabel('Year')
      .fill('2026');
    await this.page
      .locator('#appeal_appellant_confidentialityRequired_No')
      .evaluate((element: HTMLInputElement) => {
        element.click();
        element.checked = true;
        element.dispatchEvent(
          new MouseEvent('click', { bubbles: true, cancelable: true })
        );
        element.dispatchEvent(new Event('input', { bubbles: true }));
        element.dispatchEvent(new Event('change', { bubbles: true }));
      });
    await this.page
      .getByRole('group', { name: 'Appellant Role (Optional)' })
      .getByLabel('Role (Optional)')
      .selectOption({ label: 'Paying parent' });
    await this.page
      .getByRole('textbox', { name: 'Child maintenance number' })
      .fill('123456');
    await this.page.getByRole('button', { name: 'Submit', exact: true }).click();
    await this.page.getByRole('button', { name: 'Submit', exact: true }).click();

    await this.summaryTab.verifyPageSectionByKeyValue(
      'Appeal status',
      'Incomplete Application'
    );

    await this.homePage.chooseEvent('Validate appeal');

    await this.page
      .getByRole('group', {
        name: 'Is a waiver being issued for the case?'
      })
      .getByLabel('No')
      .check();

    await this.page.getByRole('button', { name: 'Continue' }).click();
    await this.page.getByRole('button', { name: 'Submit', exact: true }).click();

    await this.waitForAwaitOtherPartyData();
    await this.waitForRequestOtherPartyDataHistory();
  }
}

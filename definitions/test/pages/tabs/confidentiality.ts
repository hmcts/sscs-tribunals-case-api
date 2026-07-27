import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
import { HomePage } from '../common/homePage';

let webActions: WebAction;

export class Confidentiality {
  readonly page: Page;
  protected homePage: HomePage;

  constructor(page: Page) {
    this.page = page;
    this.homePage = new HomePage(this.page);
    webActions = new WebAction(this.page);
  }

  async verifyConfidentialityRows(
    rows: Array<{
      party: string;
      name: string;
      status: string;
      confirmed?: 'blank' | 'today';
    }>
  ) {
    const todayDate = this.getTodayLondonDate().replace(
      /[.*+?^${}()|[\]\\]/g,
      '\\$&'
    );
    const todayPattern = new RegExp(
      `^${todayDate}, \\d{1,2}:\\d{2}(?::\\d{2})? [ap]m$`,
      'i'
    );

    for (let i = 0; i < rows.length; i++) {
      const cells = await this.getConfidentialityRowCells(i);
      expect(cells[0]).toBe(rows[i].party);
      expect(cells[1]).toBe(rows[i].name);
      expect(cells[2]).toBe(rows[i].status);

      if (rows[i].confirmed === 'blank') {
        expect(cells[3] || '').toBe('');
      } else if (rows[i].confirmed === 'today') {
        expect(cells[3] || '').toMatch(todayPattern);
      }
    }
  }

  private async getConfidentialityRowCells(rowIndex: number) {
    const table = this.page
      .locator('table:visible')
      .filter({
        has: this.confidentialityStatusHeader()
      })
      .last();
    const row = table.locator('tr').nth(rowIndex + 1);
    await expect(row).toBeVisible();
    const cellTexts = await row.locator('td').allTextContents();
    return cellTexts.map((text) => text.replace(/\s+/g, ' ').trim());
  }

  confidentialityStatusHeader() {
    return this.page
      .locator('th')
      .filter({ hasText: /^Confidentiality Status$/ });
  }

  private getTodayLondonDate() {
    return new Intl.DateTimeFormat('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      timeZone: 'Europe/London'
    }).format(new Date());
  }

  async verifyConfidentialityFlagVisibility(visible: boolean) {
    const locator = '#isConfidentialCaseLabel img';

    if (visible) {
      await webActions.verifyElementVisibility(locator);
    } else {
      await webActions.verifyElementHidden(locator);
    }
  }
}

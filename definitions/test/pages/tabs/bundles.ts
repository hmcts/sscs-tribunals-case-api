import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
import { HomePage } from '../common/homePage';

let webActions: WebAction;

export class Bundles {
  readonly page: Page;
  protected homePage: HomePage;

  constructor(page: Page) {
    this.page = page;
    this.homePage = new HomePage(this.page);
    webActions = new WebAction(this.page);
  }

  async verifyBundleSubmit() {
    await webActions.clickSubmitButton();
  }

  async verifyBundlesTabContentByKeyValueForASpan(
    fieldLabel: string,
    fieldValue: string
  ): Promise<void> {
    const locator = this.page
      .locator(
        `//*[normalize-space()="${fieldLabel}"]/..//span//span[normalize-space()="${fieldValue}"]`
      )
      .first();
    try {
      await expect(locator).toBeVisible();
    } catch (error) {
      await this.page.reload();
      await this.homePage.navigateToTab('Bundles');
    }
    await expect(locator).toBeVisible();
  }

  async verifyEditedBundlesTabContentByKeyValueForASpan(
    fieldLabel: string,
    fieldValue: string,
    index: number
  ): Promise<void> {
    const locator = this.page
      .locator(
        `//*[normalize-space()="${fieldLabel}"]/..//span//span[normalize-space()="${fieldValue}"]`
      )
      .nth(index);
    try {
      await expect(locator).toBeVisible();
    } catch (error) {
      await this.page.reload();
      await this.homePage.navigateToTab('Bundles');
    }
    await expect(locator).toBeVisible();
  }

  async verifyTableElementByIndex(
    fieldLabel: string,
    fieldValue: string,
    index: number
  ): Promise<void> {
    const locator = this.page
      .locator(
        `//*[normalize-space()="${fieldLabel}"]/..//span//*[normalize-space()="${fieldValue}"]`
      )
      .nth(index);
    await expect(locator).toBeVisible();
  }

  async verifyBundlesTabContentByKeyValueForASpanRegEx(
    fieldLabel: string,
    fieldValueRegex: string
  ): Promise<void> {
    const isVisible = await this.page.evaluate(
      ({ fieldLabel, fieldValueRegex }) => {
        const labelElement = Array.from(document.querySelectorAll('span')).find(
          (el) => el.textContent.trim() === fieldLabel
        );
        if (!labelElement) return false;

        const regex = new RegExp(fieldValueRegex);
        return Array.from(
          labelElement.closest('td')?.querySelectorAll('span') || []
        ).some((span) => regex.test(span.textContent.trim()));
      },
      { fieldLabel, fieldValueRegex }
    );

    expect(isVisible).toBe(true);
  }
}

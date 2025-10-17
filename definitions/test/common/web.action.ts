import { expect, Page } from '@playwright/test';
import * as path from 'path';

export class WebAction {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async chooseOptionByLabel(elementLocator: string, labelText: string) {
    await this.verifyElementVisibility(elementLocator);
    await this.page
      .locator(elementLocator)
      .first()
      .selectOption({ label: labelText });
  }

  async chooseOptionByIndex(elementLocator: string, indexNum: number) {
    await this.verifyElementVisibility(elementLocator);
    await this.page
      .locator(elementLocator)
      .first()
      .selectOption({ index: indexNum });
  }

  async verifyPageLabel(elementLocator: string, labelText: string | string[]) {
    if (labelText instanceof Array) {
      await Promise.all(
        labelText.map(async (text, i) => {
          await expect(this.page.locator(elementLocator).nth(i)).toBeVisible();
          await expect(this.page.locator(elementLocator).nth(i)).toHaveText(text);
        })
      );
    } else {
      await this.verifyElementVisibility(elementLocator);
      await expect(this.page.locator(elementLocator).first())
        .toHaveText(labelText);
    }
  }

  async verifyTotalElements(elementLocator: string, eleCount: number) {
    await expect(this.page.locator(elementLocator)).toHaveCount(eleCount);
  }

  async verifyTextVisibility(labelText: string) {
    await expect(this.page.getByText(labelText).first()).toBeVisible();
  }

  async verifyTextVisibilityFastFail(labelText: string) {
    await expect(this.page.getByText(labelText).first()).toBeVisible({timeout: 5000});
  }

  async verifyElementVisibility(elementlocator: string) {
    await expect(this.page.locator(elementlocator).first()).toBeVisible();
  }

  async verifyElementHidden(elementlocator: string) {
    await expect(this.page.locator(elementlocator).first()).toBeHidden();
  }

  async inputField(elementLocator: string, inputValue: string) {
    await this.verifyElementVisibility(elementLocator);
    await expect(this.page.locator(elementLocator).first()).toBeEnabled();
    await this.page.locator(elementLocator).first().fill(inputValue);
  }

  async clearInputField(elementLocator: string) {
    await this.verifyElementVisibility(elementLocator);
    await this.page.locator(elementLocator).first().clear();
  }

  async clickFindButton(): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
    await this.page
      .locator('//exui-case-reference-search-box/div/form/button')
      .first()
      .click();
  }

  async clickApplyFilterButton(): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
    await this.verifyElementVisibility('//button[@title=\'Apply filter\']');
    await this.page.locator('//button[@title=\'Apply filter\']').first().click();
  }

  async clickButton(elementLocator: string): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
    await expect(this.page.getByRole('button', { name: elementLocator, exact: true }).first())
      .toBeVisible();
    await expect(this.page.getByRole('button', { name: elementLocator, exact: true }).first())
      .toBeEnabled();
    await this.page
      .getByRole('button', { name: elementLocator, exact: true })
      .first()
      .click();
  }

  async clickGoButton(elementLocator: string): Promise<void> {
    await this.page
      .getByRole('button', { name: elementLocator })
      .first()
      .dispatchEvent('click');
  }

  async clickSubmitButton(): Promise<void> {
    await this.verifyElementVisibility('//*[@class=\'button\']');
    await expect(this.page.locator('//*[@class=\'button\']').first()).toBeEnabled();
    await this.page.locator('//*[@class=\'button\']').first().click();
  }

  async clickRadioButton(elementLocator: string): Promise<void> {
    await expect(this.page
      .getByRole('radio', { name: elementLocator })
      .first()).toBeVisible();
    await this.page
      .getByRole('radio', { name: elementLocator })
      .first()
      .click();
  }

  async checkAnCheckBox(elementValue: string): Promise<void> {
    await this.page.getByLabel(elementValue).first().check();
  }

  async unCheckAnCheckBox(elementLocator: string): Promise<void> {
    await this.page.locator(elementLocator).uncheck();
  }

  async clickElementById(elementLocator: string): Promise<void> {
    await this.verifyElementVisibility(elementLocator);
    await this.page.locator(elementLocator).first().click();
  }

  async clickElementWithForce(elementLocator: string): Promise<void> {
    await this.page.locator(elementLocator).first().click({ force: true });
  }

  async clickLink(elementLocator: string): Promise<void> {
    await expect(this.page.getByRole('link', { name: elementLocator }).first()).toBeVisible();
    await this.page.getByRole('link', { name: elementLocator }).first().click();
  }

  async isLinkClickable(elementLocator: string): Promise<void> {
    await expect(this.page.getByRole('link', { name: elementLocator }).first()).toBeVisible();
    await expect(this.page
      .getByRole('link', { name: elementLocator })
      .first()
    ).toBeEnabled();
  }

  async isButtonClickable(elementLocator: string): Promise<void> {
     await expect(this.page.getByRole('button', { name: elementLocator })
     .first())
     .toBeVisible();
  }

  async clickNextStepButton(elementId: string): Promise<void> {
    await this.page.locator(elementId).first().click();
  }

  async uploadFile(elementId: string, fileName: string): Promise<void> {
    await this.page
      .locator(elementId)
      .first()
      .setInputFiles(`functional-test/data/file/${fileName}`);
  }

  async uploadFileUsingAFileChooser(
    elementId: string,
    fileName: string
  ): Promise<void> {
    await this.verifyElementVisibility(elementId);
    await this.page.locator(elementId).first()
      .setInputFiles(path.join(__dirname, `../data/file/${fileName}`));
    await this.verifyElementHidden("span:has-text('Uploading...')");
  }

  async screenshot() {
    await this.page.screenshot({
      path: 'playwright-report/screenshot.png',
      fullPage: true
    });
  }

  async delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async pause() {
    await this.page.pause();
  }
}

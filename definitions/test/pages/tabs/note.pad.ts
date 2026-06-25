import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';

let webActions: WebAction;

export class NotePad {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
    await expect(
      this.page.locator(
        `//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`
      )
    ).toBeVisible();
  }

  async verifyRequestOrReplyDeleteNoteExists(note: string, reasonForDeletion: string) {
    await webActions.verifyElementVisibility(`//span[text()='Note pad 1']`)
    await webActions.verifyElementVisibility(`//th[normalize-space()='Note']/../td//span[contains(text(), '${note}')]`);
    await webActions.verifyElementVisibility(`//th[normalize-space()='Note']/../td//span[contains(text(), '${reasonForDeletion}')]`);
  }
}

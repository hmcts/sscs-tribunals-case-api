import { expect, Locator, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
import { HomePage } from '../common/homePage';

let webActions: WebAction;

export class History {
  readonly page: Page;
  protected homePage: HomePage;

  constructor(page: Page) {
    this.page = page;
    this.homePage = new HomePage(this.page);
    webActions = new WebAction(this.page);
  }

  async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
    await expect(
      this.page.locator(
        `//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`
      )
    ).toBeVisible();
  }

  async verifyPageContentDoesNotExistByKeyValue(
    fieldLabel: string,
    fieldValue: string
  ) {
    await expect(
      this.page.locator(
        `//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`
      )
    ).toHaveCount(0);
  }

  async verifyHistoryPageContentByKeyValue(
    fieldLink: string,
    fieldLabel: string,
    fieldValue: string
  ) {
    // await expect(this.page
    //     .locator(`//*[normalize-space()="${fieldLabel}"]/../td[normalize-space()="${fieldValue}"]`)).toBeVisible();
    let eleLink = this.page.locator(`//a[normalize-space()="${fieldLink}"]`);
    let ele = this.page.locator(
      `//*[normalize-space()="${fieldLabel}"]/../td[normalize-space()="${fieldValue}"]`
    );

    for (let i = 0; i <= 5; i++) {
      try {
        await expect(eleLink).toBeVisible();
        await eleLink.click();
        await expect(ele).toBeVisible();
        break;
      } catch (error) {
        await this.homePage.navigateToTab('History');
      }
    }
  }

  async verifyHistoryPageEventLink(fieldLabel: string) {
    let linkElement = this.page.locator(
      `//a[normalize-space()="${fieldLabel}"]`
    );
    const max = 5;
    for (let i = 0; i <= max; i++) {
      try {
        await expect(linkElement).toBeVisible();
        break;
      } catch (error) {
        await this.homePage.reloadPage();
        await this.homePage.navigateToTab('History');
        if (i == max) {
          throw error;
        }
      }
    }
  }

  async verifyEventCompleted(linkText: string) {
    await expect(
      this.page.getByRole('link', { name: linkText }).first()
    ).toBeVisible();
  }

  async verifyPresenceOfTitle(fieldValue: string) {
    let text = await this.page
      .locator(`//div/markdown/h2[contains(text(),"${fieldValue}")]`)
      .textContent();
    expect(text).toContain(fieldValue); // TODO An exact match is not done as there is Text from Upper nodes of the Dom Tree Appearing.
  }

  async getDateOfEvent(): Promise<string> {
    return await this.page.locator('.EventLog-DetailsPanel .tooltip').textContent();
  }

  async getAuthorOfEvent(): Promise<string> {
    const authorSelector: Locator = this.page.locator("//table[@class='EventLogDetails']//span[text()='Author']/../following-sibling::td/span");  
    return await authorSelector.textContent();
  }
}

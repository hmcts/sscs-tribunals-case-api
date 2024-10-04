import { expect, Locator, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import allocateCtscRole from "../pages/content/allocate.ctsc.role_en.json";

let webAction: WebAction;

export class AllocateCtscRolePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('h1 span.govuk-caption-l', allocateCtscRole['allocate-ctsc-role-caption']); //Captor Text
        await webAction.verifyPageLabel('h1.govuk-heading-l', allocateCtscRole['allocate-ctsc-role-heading']); //Heading Text
        await webAction.verifyPageLabel('label[for="allocated-ctsc-caseworker', allocateCtscRole['allocate-ctsc-caseworker-label']); //Field Label
    }

    async verifyPageTitle(title: string) {
        await webAction.verifyPageLabel('h1.govuk-heading-l', title)
    }

    async allocateToUser(userEmail: string) {
        await webAction.clickRadioButton('Allocated CTSC Caseworker');
        await webAction.clickButton('Continue');
        await this.verifyPageTitle('Choose how to allocate the role');
        await webAction.clickRadioButton('Allocate to another person');
        await webAction.clickButton('Continue');
        await this.verifyPageTitle('Find the person');
        let locator = this.page.locator('#inputSelectPerson');
        await locator.click();
        await locator.pressSequentially('admin', { delay: 1000 });
        await this.page.locator(`//mat-option/span[contains(text(), '${userEmail}')]`).click();
        await webAction.clickButton('Continue');
        await webAction.clickRadioButton('7 days');
        await webAction.clickButton('Continue');
    }

    async confirmAllocation(): Promise<void> {
        await webAction.clickButton('Confirm allocation');
    }

    async verifyRoleAllocated() {
        let selector = '//exui-role-access-section[./h2[normalize-space()="CTSC"]]/exui-case-roles-table//td[normalize-space()="Allocated CTSC Caseworker"]';
        await expect(this.page.locator(selector)).toBeVisible();
    }
}
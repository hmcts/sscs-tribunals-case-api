import { expect, Page } from '@playwright/test';
import { HomePage } from '../../pages/common/homePage';
import allocateRole from '../content/allocate.role_en.json';

export class RolesAndAccess {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async verifyPageTitle(title: string) {
        await expect(this.page.locator(`h1.govuk-heading-l:has-text('${title}')`)).toBeVisible();
    }

    async allocateCtscRole(userEmail: string) {
        let homePage = new HomePage(this.page);
        homePage.navigateToTab('Roles and acesss')

        await this.page.getByRole('link', { name: allocateRole.ctscRole.link }).click();
        await this.verifyPageTitle(allocateRole.ctscRole.heading);
        await this.page.getByRole('radio', { name: allocateRole.ctscRole.ctscCaseworkerLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.ctscRole.chooseHowToAllocateRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.ctscRole.allocateToAnotherPersonRadioButtonLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.ctscRole.findPersonHeading);
        await this.page.locator('#inputSelectPerson').fill(allocateRole.ctscRole.allocateToName);
        await expect(this.page.locator('div.mat-autocomplete-panel.mat-autocomplete-visible')).toBeVisible();
        await this.page.locator(`//mat-option/span[contains(text(), '${userEmail.toLowerCase()}')]`).click();
        await expect(this.page.locator('//mat-option')).toBeHidden();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.ctscRole.durationOfRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.ctscRole.numberOfDaysRadioButtonLabel, exact: true }).click();
        await this.clickContinue();

        await expect(this.page.locator(`h1.govuk-heading-l:has-text('Check your answers')`)).toBeVisible();
        await expect(this.page.locator(`dd:has-text('${userEmail.toLowerCase()}')`)).toBeVisible();
        await this.confirmAllocation();

        await expect(this.page.locator('//h2[normalize-space()=\'Roles and access\']')).toBeVisible();
        await expect(this.page.locator('exui-role-access-section[title=\'CTSC\'] table')).toBeVisible();
    }

    async allocateInterlocutoryJudge(userEmail: string) : Promise<void> {
        let homePage = new HomePage(this.page);
        homePage.navigateToTab('Roles and acesss')

        await this.page.getByRole('link', { name: allocateRole.judicialRole.link }).click();
        await this.verifyPageTitle(allocateRole.judicialRole.heading);
        await this.page.getByRole('radio', { name: allocateRole.judicialRole.interlocutoryJudge }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.judicialRole.chooseHowToAllocateRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.judicialRole.allocateToAnotherPersonRadioButtonLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.judicialRole.findPersonHeading);
        await this.page.locator('#inputSelectPerson').fill(allocateRole.judicialRole.allocateToName.salariedJudge);
        await expect(this.page.locator('div.mat-autocomplete-panel.mat-autocomplete-visible')).toBeVisible();
        await this.page.locator(`//mat-option/span[contains(text(), '${userEmail.toLowerCase()}')]`).click();
        await expect(this.page.locator('//mat-option')).toBeHidden();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.judicialRole.durationOfRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.judicialRole.numberOfDaysRadioButtonLabel, exact: true }).click();
        await this.clickContinue();

        await expect(this.page.locator(`h1.govuk-heading-l:has-text('${allocateRole.judicialRole.cyaHeading}')`)).toBeVisible();
        await expect(this.page.locator(`dd:has-text('${userEmail.toLowerCase()}')`)).toBeVisible();
        await this.confirmAllocation();

        await expect(this.page.locator(`//h2[normalize-space()='${allocateRole.RolesAndAccessHeading}']`)).toBeVisible();
        await expect(this.page.locator('exui-role-access-section[title=\'Judiciary\'] table')).toBeVisible();
    }

    async allocateHearingJudge(userEmail: string): Promise<void> {
        let homePage = new HomePage(this.page);
        homePage.navigateToTab('Roles and acesss')

        await this.page.getByRole('link', { name: allocateRole.judicialRoleHearingJudge.link }).click();
        await this.verifyPageTitle(allocateRole.judicialRoleHearingJudge.heading);
        await this.page.getByLabel('Hearing Judge', { exact: true }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.judicialRole.chooseHowToAllocateRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.judicialRoleHearingJudge.allocateToAnotherPersonRadioButtonLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.judicialRoleHearingJudge.findPersonHeading);
        await this.page.locator('#inputSelectPerson').fill(allocateRole.judicialRoleHearingJudge.allocateToName.salariedJudge);
        await expect(this.page.locator('div.mat-autocomplete-panel.mat-autocomplete-visible')).toBeVisible();
        await this.page.locator(`//mat-option/span[contains(text(), '${userEmail.toLowerCase()}')]`).click();
        await expect(this.page.locator('//mat-option')).toBeHidden();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.judicialRoleHearingJudge.durationOfRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.judicialRoleHearingJudge.numberOfDaysRadioButtonLabel, exact: true }).click();
        await this.clickContinue();

        await expect(this.page.locator(`h1.govuk-heading-l:has-text('${allocateRole.judicialRoleHearingJudge.cyaHeading}')`)).toBeVisible();
        await expect(this.page.locator(`dd:has-text('${userEmail.toLowerCase()}')`)).toBeVisible();
        await this.confirmAllocation();

        await expect(this.page.locator(`//h2[normalize-space()='${allocateRole.RolesAndAccessHeading}']`)).toBeVisible();
        await expect(this.page.locator('exui-role-access-section[title=\'Judiciary\'] table')).toBeVisible();
    }

    async allocateAdminRole(userEmail: string) {
        let homePage = new HomePage(this.page);
        homePage.navigateToTab('Roles and acesss')

        await this.page.getByRole('link', { name: allocateRole.adminRole.link }).click();
        await this.verifyPageTitle(allocateRole.ctscRole.heading);
        await this.page.getByRole('radio', { name: allocateRole.adminRole.adminCaseworkerLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.ctscRole.chooseHowToAllocateRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.adminRole.allocateToAnotherPersonRadioButtonLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.adminRole.findPersonHeading);
        await this.page.locator('#inputSelectPerson').fill(allocateRole.adminRole.allocateToName);
        await expect(this.page.locator('div.mat-autocomplete-panel.mat-autocomplete-visible')).toBeVisible();
        await this.page.locator(`//mat-option/span[contains(text(), '${userEmail.toLowerCase()}')]`).click();
        await expect(this.page.locator('//mat-option')).toBeHidden();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.adminRole.durationOfRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.adminRole.numberOfDaysRadioButtonLabel, exact: true }).click();
        await this.clickContinue();

        await expect(this.page.locator(`h1.govuk-heading-l:has-text('Check your answers')`)).toBeVisible();
        await expect(this.page.locator(`dd:has-text('${userEmail.toLowerCase()}')`)).toBeVisible();
        await this.confirmAllocation();

        await expect(this.page.locator('//h2[normalize-space()=\'Roles and access\']')).toBeVisible();
        await expect(this.page.locator('exui-role-access-section[title=\'Admin\'] table')).toBeVisible();
    }

    async allocateLegalOpsRole(userEmail: string) : Promise<void> {
        let homePage = new HomePage(this.page);
        homePage.navigateToTab('Roles and acesss')

        await this.page.getByRole('link', { name: allocateRole.legalOpsRole.link }).click();
        await this.verifyPageTitle(allocateRole.legalOpsRole.heading);
        await this.page.getByRole('radio', { name: allocateRole.legalOpsRole.legalOpsCaseworkerLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.legalOpsRole.chooseHowToAllocateRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.legalOpsRole.allocateToAnotherPersonRadioButtonLabel }).click();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.legalOpsRole.findPersonHeading);
        await this.page.locator('#inputSelectPerson').fill(allocateRole.legalOpsRole.allocateToName);
        await expect(this.page.locator('div.mat-autocomplete-panel.mat-autocomplete-visible')).toBeVisible();
        await this.page.locator(`//mat-option/span[contains(text(), '${userEmail.toLowerCase()}')]`).click();
        await expect(this.page.locator('//mat-option')).toBeHidden();
        await this.clickContinue();

        await this.verifyPageTitle(allocateRole.legalOpsRole.durationOfRoleHeading);
        await this.page.getByRole('radio', { name: allocateRole.legalOpsRole.numberOfDaysRadioButtonLabel, exact: true }).click();
        await this.clickContinue();

        await expect(this.page.locator(`h1.govuk-heading-l:has-text('Check your answers')`)).toBeVisible();
        await expect(this.page.locator(`dd:has-text('${userEmail.toLowerCase()}')`)).toBeVisible();
        await this.confirmAllocation();

        await expect(this.page.locator(`//h2[normalize-space()='${allocateRole.RolesAndAccessHeading}']`)).toBeVisible();
        await expect(this.page.locator('exui-role-access-section[title=\'Legal Ops\'] table')).toBeVisible();
    }

    async confirmAllocation(): Promise<void> {
        await this.page.getByRole('button', { name: 'Confirm allocation' }).click();
    }

    async clickContinue(): Promise<void> {
        await this.page.getByRole('button', { name: 'Continue' }).click();
    }

    async verifyRoleAllocated() {
        let selector = '//exui-role-access-section[./h2[normalize-space()="CTSC"]]/exui-case-roles-table//td[normalize-space()="Allocated CTSC Caseworker"]';
        await expect(this.page.locator(selector)).toBeVisible();
    }
}

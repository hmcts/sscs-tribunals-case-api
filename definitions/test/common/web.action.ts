import {expect, Page} from '@playwright/test';
import logger from '../utils/loggerUtil';
import path from 'path';

export class WebAction {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async chooseOptionByLabel(elementLocator: string, labelText: string) {
        await this.page
            .locator(elementLocator)
            .first()
            .selectOption({label: labelText})
    }

    async chooseOptionByIndex(elementLocator: string, indexNum: number) {
        await this.page
            .locator(elementLocator)
            .first()
            .selectOption({index: indexNum})
    }

    async verifyPageLabel(elementLocator: string, labelText: string | string[]) {
        await expect(this.page.locator(elementLocator))
            .first()
            .toHaveText(labelText)
    }

    async verifyTotalElements(elementLocator: string, eleCount: number) {
        await expect(this.page.locator(elementLocator))
            .toHaveCount(eleCount)
    }

    async verifyTextVisibility(labelText: string) {
        await expect(this.page.getByText(labelText))
            .first()
            .toBeVisible()
    }

    async verifyElementVisibility(elementlocator: string) {
        await expect(this.page.locator(elementlocator))
            .first()
            .toBeVisible()
    }

    async typeField(elementLocator: string, inputValue: string) {
        await this.page
            .type(elementLocator, inputValue)
    }

    async inputField(elementLocator: string, inputValue: string) {
        await this.page
            .fill(elementLocator, inputValue)
    }

    async clearInputField(elementLocator: string) {
        await this.page
            .locator(elementLocator)
            .first()
            .clear()
    }

    async clickFindButton(): Promise<void> {
        await this.page.waitForLoadState('domcontentloaded');
        await this.page
            .locator("//exui-case-reference-search-box/div/form/button")
            .first()
            .click()
    }

    async clickApplyFilterButton(): Promise<void> {
        await this.page.waitForLoadState('domcontentloaded');
        await this.page
            .locator("//button[@title='Apply filter']")
            .first()
            .click()
    }

    async clickButton(elementLocator: string): Promise<void> {
        await this.page.waitForLoadState('domcontentloaded');
        await this.page.getByRole('button', {name: elementLocator}).first().waitFor();
        await this.page
            .getByRole('button', {name: elementLocator, exact: true})
            .first()
            .click({force: true})
    }

    async clickGoButton(elementLocator: string): Promise<void> {
        await this.page
            .getByRole('button', {name: elementLocator})
            .first()
            .dispatchEvent('click')
    }

    async clickSubmitButton(): Promise<void> {
        await this.page
            .locator("//*[@class='button']")
            .first()
            .click()
    }

    async clickRadioButton(elementLocator: string): Promise<void> {
        await this.page
            .getByRole('radio', {name: elementLocator})
            .first()
            .click()
    }

    async checkAnCheckBox(elementValue: string): Promise<void> {
        await this.page
            .getByLabel(elementValue)
            .first()
            .check()
    }

    async clickElementById(elementLocator: string): Promise<void> {
        await this.page
            .locator(elementLocator)
            .first()
            .click()
    }

    async clickElementWithForce(elementLocator: string): Promise<void> {
        await this.page
            .locator(elementLocator)
            .first()
            .click({force: true})
    }

    async clickLink(elementLocator: string): Promise<void> {
        await this.page
            .getByRole('link', {name: elementLocator})
            .first()
            .click()
    }

    async isLinkClickable(elementLocator: string): Promise<void> {
        await this.page
            .getByRole('link', {name: elementLocator})
            .first()
            .isEnabled()
    }

    async clickNextStepButton(elementId: string): Promise<void> {
        await this.page
            .first()
            .click(elementId)
    }

    async uploadFile(elementId: string, fileName: string): Promise<void> {
        await this.page
            .locator(elementId)
            .first()
            .setInputFiles(`functional-test/data/file/${fileName}`)
    }

    async uploadFileUsingAFileChooser(elementId: string, fileName: string): Promise<void> {
        const fileChooserPromise = this.page.waitForEvent('filechooser');
        await this.page.locator(elementId).first().click();
        const fileChooser = await fileChooserPromise;
        await fileChooser.setFiles(path.join(__dirname, `../data/file/${fileName}`));
    }

    async screenshot() {
        await this.page.screenshot({path: 'playwright-report/screenshot.png', fullPage: true});
    }

    async delay(ms: number) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    async pause() {
        await this.page.pause();
    }
}

import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
import dateUtilsComponent from '../../utils/DateUtilsComponent';

let webAction: WebAction;
const currentDate: Date = new Date();
const currentMonth: number = currentDate.getMonth();

export class Summary {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`)).toBeVisible();
    }

    async verifyPageContentByKeyValueDoesNotExist(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`)).toHaveCount(0);
    }

    async verifyFieldHiddenInPageContent(fieldLabel: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]`)).toBeHidden();
    }

    async verifyPageContentLinkTextByKeyValue(fieldLabel: string, fieldValue: string) {
        let text = await this.page.locator(`//*[normalize-space()="${fieldLabel}"]/../..//td//a`).textContent();
        expect(text).toEqual(fieldValue);
    }

    async verifyPresenceOfText(fieldValue: string) {
        await webAction.screenshot({ fullPage: true });
        let text = await this.page.locator(`//div/markdown/p[contains(text(),"${fieldValue}")]`).textContent()
        expect(text).toContain(fieldValue); // TODO An exact match is not done as there is Text from Upper nodes of the Dom Tree Appearing.
    }

    async verifyPresenceOfTitle(fieldValue: string) {
        let text = await this.page.locator(`//div/markdown/h2[contains(text(),"${fieldValue}")]`).textContent()
        expect(text).toContain(fieldValue); // TODO An exact match is not done as there is Text from Upper nodes of the Dom Tree Appearing.
    }

    async verifyAttributeValue(expTitleText: string) {
        let imgEle = await this.page.locator('//div/markdown/h2/img');
        let actTitleText = await imgEle.getAttribute('title');
        
        expect(actTitleText).toEqual(expTitleText);
    }

    async verifyTitleNotPresent(fieldLabel: string) {
        await expect(this.page
            .locator(`//div/markdown/h2[contains(text(),"${fieldLabel}")]`)).not.toBeVisible();
    }

    async verifyTotElemensOnPage(fieldLabel: string, fieldValue: string) {
        await webAction.verifyTotalElements(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`, 2);
    }

    async verifydueDates(reqField: string){
        const dueDate = new Date();
        dueDate.setDate(new Date().getDate());
        let formattedDueDate = dateUtilsComponent.formatDateToSpecifiedDateShortFormat(dueDate);

        //Java has replaced the short of September for 'en-GB' locale to be 'Sept' which is failing our tests, this regex is a workaround for that
        if(currentMonth === 8){
            formattedDueDate = formattedDueDate.replace(/\bSept\b/, "Sep");
        } 

        console.log(`New formatted date is ####### ${formattedDueDate}`);
        await this.verifyPageContentByKeyValue(reqField, formattedDueDate);
    }

    /*async verifyPresenceOfText(fieldValue: string): Promise<void>{
        await webActions.verifyTextVisibility(fieldValue);
    }*/
}

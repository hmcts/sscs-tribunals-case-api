import {expect, Page} from '@playwright/test';
import {WebAction} from '../../common/web.action';
import dateUtilsComponent from '../../utils/DateUtilsComponent';


let webActions: WebAction;
const currentDate: Date = new Date();
const currentMonth: number = currentDate.getMonth();

export class AudioVideoEvidence {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`).first()).toBeVisible();
    }

    async verifyPageContentNotPresentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`).first()).not.toBeVisible();
    }

    async verifyTitle(fieldLabel: string) {
        await expect(this.page
            .locator(`//span[normalize-space()="${fieldLabel}"]`)).toBeVisible();
    }

    async verifyTitleNotPresent(fieldLabel: string) {
        await expect(this.page
            .locator(`//span[normalize-space()="${fieldLabel}"]`)).not.toBeVisible();
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
}

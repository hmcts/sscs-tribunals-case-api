import {expect, Page} from "@playwright/test";
import {WebAction} from "../common/web.action";

let webAction: WebAction;

export class ListingRequirementPage {

    readonly page

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page)
    }

    async updateHearingValues() {
        await webAction.typeField('#overrideFields_duration', '120');
        await webAction.clickElementById('#overrideFields_appellantInterpreter_isInterpreterWanted_Yes');
        await webAction.chooseOptionByLabel('#overrideFields_appellantInterpreter_interpreterLanguage', 'Dutch');
        await webAction.clickButton('Continue');
    }

    async submitUpdatedValues() {
        await webAction.clickElementById('#amendReasons-adminreq');
        await webAction.clickSubmitButton();
        await webAction.clickSubmitButton();
    }
}
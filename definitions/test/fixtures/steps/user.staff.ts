import {Page, expect} from '@playwright/test';
import {BaseStep} from './base';
import {credentials} from "../../config/config";

export class UserStaff extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performAddUserStaff(): Promise<void> {

        await this.loginUserWithoutCaseId(credentials.userStaffAdmin, false);
        await this.homePage.clickStaffLink();
        await new Promise(f => setTimeout(f, 3000)); //Delay required for the Page to Load
        await this.userStaffPage.verifyUserSearchPageContent();
        await this.userStaffPage.clickAddNewUser();
        await this.userStaffPage.verifyAddUserPageContent();
        //TODO - As the Location API is not populated with Values for its response.The Add User Journey cannot be carried on from here.
    }

    async performSearchAndUpdateUserStaff(): Promise<void> {

    }
}

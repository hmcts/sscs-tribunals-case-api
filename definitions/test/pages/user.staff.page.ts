import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';


let webActions: WebAction;

export class UserStaffPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyUserSearchPageContent() {
        await webActions.verifyPageLabel('.govuk-heading-xl', 'User list'); //Captor Text
        //await webActions.verifyPageLabel('h1', casereference+": Bloggs"); //Captor Text
        await webActions.verifyPageLabel('.govuk-heading-l', 'User search'); //Heading Text
        await webActions.verifyPageLabel('[_ngcontent-ng-c547129853]', 'Please select filters and click Search'); //Field Label
    }

    async clickAddNewUser(): Promise<void> {
        await webActions.clickButton('Add new user');
    }

    async verifyAddUserPageContent(): Promise<void> {
        await webActions.verifyPageLabel('.govuk-heading-xl', 'Add user'); //Captor Text
        await webActions.verifyPageLabel('#main-content > [_ngcontent-ng-c4059940359] > .govuk-body', 'Enter the details of the user you want to add.');

        await webActions.verifyPageLabel('[novalidate][_ngcontent-ng-c4059940359] > .govuk-fieldset__legend > .govuk-heading-m', 'Personal Information');
        await webActions.verifyPageLabel('[novalidate][_ngcontent-ng-c4059940359] > div:nth-of-type(1) > .govuk-label', 'First name');
        await webActions.verifyPageLabel('[novalidate][_ngcontent-ng-c4059940359] > div:nth-of-type(2) > .govuk-label', 'Last name');
        await webActions.verifyPageLabel('[novalidate][_ngcontent-ng-c4059940359] > div:nth-of-type(3) > .govuk-label', 'Email');

        await webActions.verifyPageLabel('[novalidate][_ngcontent-ng-c4059940359] > div:nth-of-type(4) .govuk-heading-m', 'Region');

        await webActions.verifyPageLabel('#services .govuk-heading-m', 'Service');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceAAA6\']', 'Specified Money Claims');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceAAA7\']', 'Damages');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceABA3\']', 'Family Public Law');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceABA5\']', 'Family Private Law');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceBBA2\']', 'Criminal Injuries Compensation');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceBBA3\']', 'Social Security and Child Support');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceBFA1\']', 'Immigration and Asylum Appeals');
        await webActions.verifyPageLabel('[for=\'checkbox_serviceBHA1\']', 'Employment Claims');

        await webActions.verifyPageLabel('#base_locations .govuk-heading-m', 'Location');
        await webActions.verifyPageLabel('#base_locations_primary > .govuk-heading-s', 'Primary location');
        await webActions.verifyPageLabel('p[_ngcontent-ng-c4059940359]', 'A user can only have one primary location.');
        await webActions.verifyPageLabel('#base_locations_primary .govuk-body', 'Enter a location name');
        await webActions.verifyPageLabel('#base_locations_additional > .govuk-heading-s', 'Additional locations (optional)');
        await webActions.verifyPageLabel('#base_locations_additional .govuk-body', 'Enter a location name');

        await webActions.verifyPageLabel('[novalidate][_ngcontent-ng-c4059940359] > div:nth-of-type(7) .govuk-heading-m','User type');
        await webActions.verifyPageLabel('#userRoles .govuk-heading-m','Role (optional)');
        await webActions.verifyPageLabel('[for=\'checkbox_userRoles_case_allocator\']','Case allocator');
        await webActions.verifyPageLabel('[for=\'checkbox_userRoles_task_supervisor\']','Task supervisor');
        await webActions.verifyPageLabel('[for=\'checkbox_userRoles_staff_admin\']','Staff administrator');

        await webActions.verifyPageLabel('#roles .govuk-heading-m','Job title');
        //await webActions.verifyPageLabel('//div[contains(@id,\'checkbox_job_title\') and contains(text(), \'IBCA Caseworker\')]','IBCA Caseworker');

        await webActions.verifyPageLabel('#skills.govuk-label','Skills (optional)');
    }

    async inputAddUserPageContent(): Promise<void> {

        await webActions.inputField('#first_name','Tester');
        await webActions.inputField('#last_name','Joe');
        await webActions.inputField('#email_id','ibca-test-caseworker@justice.gov.uk');
        await webActions.chooseOptionByLabel('#region_id','London');
        await webActions.checkAnCheckBox('[value=\'BBA3\']');
        //await webActions.inputField('//input[@class=\'mat-autocomplete-trigger govuk-input ng-valid ng-dirty ng-touched\']','East London (S)');
        await webActions.clickButton('Add new user');
        //await webActions.inputField('#base_locations_additional #location-primary','Swansea CJC');
        await webActions.clickButton('Add additional locations');
        await webActions.chooseOptionByLabel('#region_id','London');
    }

}

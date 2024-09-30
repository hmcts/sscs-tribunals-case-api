import { expect, Locator, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
import logger from '../../utils/loggerUtil';
import { environment } from "../../config/config";

const fs = require('fs');
const yaml = require('js-yaml');

let webActions: WebAction;

export class HomePage {

    readonly page: Page;
    readonly summaryTab: Locator;
    readonly notePadTab: Locator;
    readonly historyTab: Locator;
    readonly rolesAndAccessTab: Locator;
    readonly tasksTab: Locator;
    readonly welshTab: Locator;
    readonly appealDetailsTab: Locator;
    readonly bundlesTab: Locator;
    readonly submitNextStepButton: string;
    readonly nextStepDropDown: string;
    readonly eventTitle: Locator;
    readonly beforeTabBtn: Locator;
    readonly hearingRecordingsTab: Locator;
    readonly documentsTab: Locator;
    readonly listingRequirementsTab: Locator;
    readonly subscriptionsTab: Locator;
    readonly audioVideoEvidenceTab: Locator;
    readonly ftaDocumentsTab: Locator;
    readonly otherPartyDetailsTab: Locator;
    readonly hearingsTab: Locator;
    readonly afterTabBtn: Locator;
    readonly caseTypeDropdown: string;
    readonly caseRefInputField: string;
    readonly searchResultsField: string;


    constructor(page: Page) {
        this.page = page;
        this.notePadTab = page.locator('//div[contains(text(), "Notepad")]');
        this.summaryTab = page.getByRole('tab', { name: 'Summary', exact: true });
        this.historyTab = page.getByRole('tab', { name: 'History', exact: true });
        this.tasksTab = page.getByRole('tab', { name: 'Tasks', exact: true });
        this.welshTab = page.getByRole('tab', { name: 'Welsh', exact: true });
        this.rolesAndAccessTab = page.getByRole('tab', { name: 'Roles and access', exact: true });
        this.appealDetailsTab = page.getByText('Appeal Details', {exact: true});
        this.bundlesTab = page.getByText('Bundles', {exact: true});
        this.nextStepDropDown = '#next-step';
        this.submitNextStepButton = '//button[@class="submit"]';
        this.eventTitle = page.locator('h1.govuk-heading-l');
        this.hearingRecordingsTab = page.getByRole('tab', { name: 'Hearing Recordings', exact: true });
        this.documentsTab = page.getByRole('tab', { name: 'Documents', exact: true });
        this.listingRequirementsTab = page.getByRole('tab', { name: 'Listing Requirements', exact: true });
        this.audioVideoEvidenceTab = page.getByRole('tab', { name: 'Audio/Video evidence', exact: true });
        this.beforeTabBtn = page.locator('//html/body/exui-root/exui-case-home/div/exui-case-details-home/exui-case-viewer-container/ccd-case-viewer/div/ccd-case-full-access-view/div[2]/div/mat-tab-group/mat-tab-header/button[1]/div');
        this.subscriptionsTab = page.getByRole('tab', { name: 'Subscriptions', exact: true });
        this.ftaDocumentsTab = page.getByRole('tab', { name: 'FTA Documents', exact: true });
        this.otherPartyDetailsTab = page.getByRole('tab', { name: 'Other Party Details', exact: true });
        this.hearingsTab = page.getByRole('tab', { name: 'Hearings', exact: true })
        this.afterTabBtn = page.locator('//html/body/exui-root/exui-case-home/div/exui-case-details-home/exui-case-viewer-container/ccd-case-viewer/div/ccd-case-full-access-view/div[2]/div/mat-tab-group/mat-tab-header/button[2]/div');
        this.caseTypeDropdown = '#s-case-type';
        this.caseRefInputField = '//*[@id="[CASE_REFERENCE]"]';
        this.searchResultsField = '#search-result > table > tbody > tr > td:nth-child(1) >';


        webActions = new WebAction(this.page);

    }

    async delay(ms: number) {
        return new Promise( resolve => setTimeout(resolve, ms) );
    }

    async reloadPage() {
        await this.page.reload({timeout:13000, waitUntil:'load'});
    }

    async signOut(): Promise<void> {
        await webActions.clickElementById("//a[contains(.,'Sign out')]");
    }

    async goToHomePage(caseId: string): Promise<void> {
        await this.findAndNavigateToCase(caseId);
    }

    async searchCaseWithAATDef() {
        const optionToSelect = await this.page.locator('option', { hasText: `SSCS Case ${environment.aatDefVersion.TAG} AAT` }).textContent();
        console.log(`case type dropdown value is ###### ${optionToSelect}`);
        await webActions.chooseOptionByLabel(this.caseTypeDropdown, optionToSelect);
    }

    async searchCaseWithPreviewDef() {
        const optionToSelect = await this.page.locator('option', { hasText: `SSCS Case ${environment.aatDefVersion.TAG} PR` }).textContent();
        console.log(`case type dropdown value is ###### ${optionToSelect}`);
        await webActions.chooseOptionByLabel(this.caseTypeDropdown, optionToSelect);
    }

    async findAndNavigateToCase(caseId: string): Promise<void> {
        await this.page.getByRole('link', { name: 'Find case' }).waitFor();
        await this.page.getByRole('link', { name: 'Find case' }).click();
        await this.delay(3000);
        await expect(this.page.getByText('Filters')).toBeVisible();
        console.log(`url of the page is ######## ${this.page.url()}`);
        const expUrl = this.page.url();
        
        if(environment.name == 'preview') {
            
            if(environment.hearingsEnabled == 'Yes') {
                let matches = expUrl.match(/(\d+)/);
                let PrNo = matches[0];
                console.log(`PR number on url is ###### ${PrNo}`);

                const optionToSelect = await this.page.locator('option', { hasText: PrNo }).textContent();
                console.log(`case type dropdown value is ###### ${optionToSelect}`);
                await webActions.chooseOptionByLabel(this.caseTypeDropdown, optionToSelect);
            } else {
                await this.searchCaseWithPreviewDef();
            }
            
        } else if(environment.name == 'aat') {

            await this.searchCaseWithAATDef();
        } else {
            logger.info('No environment variable is set');
        }

        await webActions.inputField(this.caseRefInputField, caseId);
        await webActions.clickApplyFilterButton();

        await this.delay(3000);
        await webActions.verifyTotalElements(`#search-result > table > tbody > tr > td:nth-child(1) > a[href='/cases/case-details/${caseId}']`, 1);
        await webActions.verifyElementVisibility(`#search-result > table > tbody > tr > td:nth-child(1) > a[href='/cases/case-details/${caseId}']`);
        await webActions.clickElementById(`#search-result > table > tbody > tr > td:nth-child(1) > a[href='/cases/case-details/${caseId}']`);

        await this.delay(3000);
        await expect(this.summaryTab)
            .toBeVisible()
            .catch((error) => {
                logger.error(`Element to verify assertion is not present: ${error}`);
            });
    }

    async goToCaseList(): Promise<void> {
        //await this.page.goto(`/cases`);
        await this.selectToViewTasksAndCasesIfRequired();
        await this.page.getByRole('link', { name: 'Case list' }).waitFor();
        await this.page.getByRole('link', { name: 'Case list' }).click();
        await this.delay(3000);
        await expect(this.page.getByText('Filters')).toBeVisible();
    }

    async chooseEvent(eventName: string): Promise<void> {
        await this.delay(3000);
        await webActions.chooseOptionByLabel(this.nextStepDropDown, eventName);
        await expect(this.page.getByRole('button', { name: 'Go', exact: true })).toBeEnabled();
        await this.delay(5000);
        await webActions.clickSubmitButton();
        // await webActions.clickNextStepButton(this.submitNextStepButton);
        // await webActions.clickGoButton('Go');
    }

    async clickBeforeTabBtn(): Promise<void> {
        await this.beforeTabBtn.click();
    }

    async clickAfterTabBtn(): Promise<void> {
        await this.afterTabBtn.click();
    }

    async waitForLoadState() {
        await this.page.waitForLoadState('networkidle');
    }

    async clickSignOut() {
        await webActions.clickElementById('li a.hmcts-header__navigation-link');
    }

    async selectToViewTasksAndCasesIfRequired() {
        expect(await this.page.locator('h1').count()).toBeGreaterThanOrEqual(1);
        let headerText = await this.page.locator('h1').first().textContent();
        if(headerText.toLowerCase().includes('work access')) {
            await this.page.getByRole('radio', { name: 'View tasks and cases' }).click();
            await this.page.getByRole('button', { name: 'Continue' }).click();
        }
    }

    async finishLoadingThePage() {
        await expect(this.page.locator('.spinner-container')).toBeDisabled({timeout:4000});
    }

    async navigateToTab(tabName : string): Promise<void> {
        switch(tabName) {
            case "Notepad": {
                await this.notePadTab.click();
                break;
            }
            case "History": {
                await expect(this.historyTab).toBeVisible();
                await this.historyTab.click();
                break;
            }
            case "Summary": {
                if (expect(this.summaryTab).toBeVisible()){
                    await this.summaryTab.click();
                } else {
                    await this.clickBeforeTabBtn();
                    await this.summaryTab.click();
                }
                break;
            }
            case "Tasks": {
                await expect(this.tasksTab).toBeVisible();
                await this.tasksTab.click();
                break;
            }
            case "Welsh": {
                await expect(this.welshTab).toBeVisible();
                await this.welshTab.click();
                break;
            }
            case "Roles and access": {
                await expect(this.rolesAndAccessTab).toBeVisible();
                await this.rolesAndAccessTab.click();
                break;
            }
            case "Appeal Details": {
                await expect(this.appealDetailsTab).toBeVisible();
                await this.appealDetailsTab.click();
                break;
            }
            case "Bundles": {
                await expect(this.bundlesTab).toBeVisible();
                await this.bundlesTab.click();
                break;
            }
            case "Hearing Recordings": {
                await expect(this.hearingRecordingsTab).toBeVisible({ timeout: 8000});
                await this.hearingRecordingsTab.click();
                break;
            }
            case "Documents": {
                await expect(this.documentsTab).toBeVisible();
                await this.documentsTab.click();
                break;
            }
            case "Listing Requirements": {
                await expect(this.listingRequirementsTab).toBeVisible();
                await this.listingRequirementsTab.click();
                break;
            }
            case "Audio/Video Evidence": {
                await expect(this.audioVideoEvidenceTab).toBeVisible();
                await this.audioVideoEvidenceTab.click();
                break;
            }
            case "FTA Documents": {
                await expect(this.ftaDocumentsTab).toBeVisible();
                await this.ftaDocumentsTab.click();
                break;
            }
            case "Subscriptions": {
                await this.subscriptionsTab.click();
                break;
            }
            case "Other Party Details": {
                await this.otherPartyDetailsTab.click();
                break;
            }
            case "Hearings": {
                await expect(this.hearingsTab).toBeVisible();
                await this.hearingsTab.click();
                break;
            }
            default: {
                //statements;
                break;
            }
        }
    }
}

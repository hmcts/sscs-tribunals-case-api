import { expect, Locator, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
import logger from '../../utils/loggerUtil';
import { environment } from '../../config/config';

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
  readonly ftaCommunicationTab: Locator;

  constructor(page: Page) {
    this.page = page;

    this.notePadTab = page.getByRole('tab').filter({ hasText: /^Notepad$/ });
    this.summaryTab = page.getByRole('tab').filter({ hasText: /^Summary$/ });
    this.historyTab = page.getByRole('tab').filter({ hasText: /^History$/ });
    this.tasksTab = page.getByRole('tab').filter({ hasText: /^Tasks$/ });
    this.welshTab = page.getByRole('tab').filter({ hasText: /^Welsh$/ });
    this.rolesAndAccessTab = page.getByRole('tab').filter({ hasText: /^Roles and access$/ });
    this.appealDetailsTab = page.getByRole('tab').filter({ hasText: /^Appeal Details$/ });
    this.bundlesTab = page.getByRole('tab').filter({ hasText: /^Bundles$/ });
    this.nextStepDropDown = '#next-step';
    this.submitNextStepButton = '//button[@class="submit"]';
    this.eventTitle = page.locator('h1.govuk-heading-l');
    this.hearingRecordingsTab = page.getByRole('tab').filter({ hasText: /^Hearing Recordings$/ });
    this.documentsTab = page.getByRole('tab').filter({ hasText: /^Documents$/ });
    this.listingRequirementsTab = page.getByRole('tab').filter({ hasText: /^Listing Requirements$/ });
    this.audioVideoEvidenceTab = page.getByRole('tab').filter({ hasText: /^Audio\/Video evidence$/ });
    this.beforeTabBtn = page.locator(
      '//html/body/exui-root/exui-case-home/div/exui-case-details-home/exui-case-viewer-container/ccd-case-viewer/div/ccd-case-full-access-view/div[2]/div/mat-tab-group/mat-tab-header/button[1]/div'
    );
    this.subscriptionsTab = page.getByRole('tab').filter({ hasText: /^Subscriptions$/ });
    this.ftaDocumentsTab = page.getByRole('tab').filter({ hasText: /^FTA Documents$/ });
    this.otherPartyDetailsTab = page.getByRole('tab').filter({ hasText: /^Other Party Details$/ });
    this.hearingsTab = page.getByRole('tab').filter({ hasText: /^Hearings$/ });
    this.afterTabBtn = page.locator(
      '//html/body/exui-root/exui-case-home/div/exui-case-details-home/exui-case-viewer-container/ccd-case-viewer/div/ccd-case-full-access-view/div[2]/div/mat-tab-group/mat-tab-header/button[2]/div'
    );
    this.caseTypeDropdown = '#s-case-type';
    this.caseRefInputField = '//*[@id="[CASE_REFERENCE]"]';
    this.searchResultsField =
      '#search-result > table > tbody > tr > td:nth-child(1) >';
    this.ftaCommunicationTab = page.getByRole('tab').filter({ hasText: /^FTA Communcations$/});

    webActions = new WebAction(this.page);
  }

  async delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async reloadPage() {
    await this.page.reload({ timeout: 13000, waitUntil: 'load' });
  }

  async signOut(): Promise<void> {
    const pageUrl = this.page.url();
    await webActions.clickElementById("//a[contains(.,'Sign out')]");
    await expect(this.page).not.toHaveURL(pageUrl);
  }

  async goToHomePage(caseId: string): Promise<void> {
    await this.findAndNavigateToCase(caseId);
  }

  async searchCaseWithAATDef() {
    const optionToSelect = await this.page
      .locator('option', {
        hasText: `SSCS Case ${environment.aatDefVersion.TAG} AAT`
      })
      .textContent();
    logger.debug(`case type dropdown value is ###### ${optionToSelect}`);
    await webActions.chooseOptionByLabel(this.caseTypeDropdown, optionToSelect);
  }

  async searchCaseWithPreviewDef() {
    const optionToSelect = await this.page
      .locator('option', {
        hasText: `SSCS Case ${environment.aatDefVersion.TAG} PR`
      })
      .textContent();
    logger.debug(`case type dropdown value is ###### ${optionToSelect}`);
    await webActions.chooseOptionByLabel(this.caseTypeDropdown, optionToSelect);
  }

  async findAndNavigateToCase(caseId: string): Promise<void> {
    await this.page.getByRole('link', { name: 'Find case' }).first().waitFor();
    await this.page.getByRole('link', { name: 'Find case' }).first().click();
    await this.delay(3000);
    await expect(this.page.getByText('Filters').first()).toBeVisible();
    logger.debug(`url of the page is ######## ${this.page.url()}`);
    const expUrl = this.page.url();

    if (environment.name == 'pr') {
      await this.searchCaseWithPreviewDef();
    } else if (environment.name == 'aat') {
      await this.searchCaseWithAATDef();
    } else {
      logger.info('No environment variable is set');
    }

    await webActions.inputField(this.caseRefInputField, caseId);
    await webActions.clickApplyFilterButton();

    await this.delay(3000);
    await webActions.verifyTotalElements(
      `#search-result > table > tbody > tr > td:nth-child(1) > a[href='/cases/case-details/${caseId}']`,
      1
    );
    await webActions.verifyElementVisibility(
      `#search-result > table > tbody > tr > td:nth-child(1) > a[href='/cases/case-details/${caseId}']`
    );
    await webActions.clickElementById(
      `#search-result > table > tbody > tr > td:nth-child(1) > a[href='/cases/case-details/${caseId}']`
    );

    await expect(this.summaryTab.first()).toBeVisible();
  }

  async goToCaseList(): Promise<void> {
    //await this.page.goto(`/cases`);
    await this.selectToViewTasksAndCasesIfRequired();
    await this.page.getByRole('link', { name: 'Case list' }).first().waitFor();
    await this.page.getByRole('link', { name: 'Case list' }).first().click();
    await this.delay(3000);
    await expect(this.page.getByText('Filters').first()).toBeVisible();
  }

  async chooseEvent(eventName: string): Promise<void> {
    await this.delay(3000);
    await webActions.chooseOptionByLabel(this.nextStepDropDown, eventName);
    await expect(
      this.page.getByRole('button', { name: 'Go', exact: true })
    ).toBeEnabled();
    await this.delay(5000);
    await webActions.clickSubmitButton();
    // await webActions.clickNextStepButton(this.submitNextStepButton);
    // await webActions.clickGoButton('Go');
  }

  async clickBeforeTabBtn(): Promise<void> {
    const isEleVisible = await this.beforeTabBtn.isVisible();

    isEleVisible
      ? await this.beforeTabBtn.click()
      : logger.info('Before tab button is not visible');
  }

  async clickAfterTabBtn(): Promise<void> {
    const isEleVisible = await this.afterTabBtn.isVisible();

    isEleVisible
      ? await this.afterTabBtn.click()
      : logger.info('After tab button is not visible');
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
    if (headerText.toLowerCase().includes('work access')) {
      await this.page
        .getByRole('radio', { name: 'View tasks and cases' })
        .click();
      await this.page.getByRole('button', { name: 'Continue' }).click();
    }
  }

  async finishLoadingThePage() {
    await expect(this.page.locator('.spinner-container')).toBeDisabled({
      timeout: 4000
    });
  }

  async navigateToTab(tabName: string): Promise<void> {
    switch (tabName) {
      case 'Notepad': {
        await this.notePadTab.click();
        break;
      }
      case 'History': {
        await expect(this.historyTab).toBeVisible();
        await this.historyTab.click();
        break;
      }
      case 'Summary': {
        try {
          await expect(this.summaryTab).toBeVisible();
          await this.summaryTab.click();
        } catch {
          await this.clickBeforeTabBtn();
          await this.summaryTab.click();
        }
        break;
      }
      case 'Tasks': {
        await expect(this.tasksTab).toBeVisible();
        await this.tasksTab.click();
        break;
      }
      case 'Welsh': {
        await expect(this.welshTab).toBeVisible();
        await this.welshTab.click();
        break;
      }
      case 'Roles and access': {
        await expect(this.rolesAndAccessTab).toBeVisible();
        await this.rolesAndAccessTab.click();
        break;
      }
      case 'Appeal Details': {
        await expect(this.appealDetailsTab).toBeVisible();
        await this.appealDetailsTab.click();
        break;
      }
      case 'Bundles': {
        await expect(this.bundlesTab).toBeVisible();
        await this.bundlesTab.click();
        break;
      }
      case 'Hearing Recordings': {
        await expect(this.hearingRecordingsTab).toBeVisible({ timeout: 8000 });
        await this.hearingRecordingsTab.click();
        break;
      }
      case 'Documents': {
        await expect(this.documentsTab).toBeVisible();
        await this.documentsTab.click();
        break;
      }
      case 'Listing Requirements': {
        await expect(this.listingRequirementsTab).toBeVisible();
        await this.listingRequirementsTab.click();
        break;
      }
      case 'Audio/Video Evidence': {
        await expect(this.audioVideoEvidenceTab).toBeVisible();
        await this.audioVideoEvidenceTab.click();
        break;
      }
      case 'FTA Documents': {
        await expect(this.ftaDocumentsTab).toBeVisible();
        await this.ftaDocumentsTab.click();
        break;
      }
      case 'Subscriptions': {
        await this.subscriptionsTab.click();
        break;
      }
      case 'Other Party Details': {
        await this.otherPartyDetailsTab.click();
        break;
      }
      case 'Hearings': {
        await expect(this.hearingsTab).toBeVisible();
        await this.hearingsTab.click();
        break;
      }
      case 'FTA Communications':{
        await expect(this.ftaCommunicationTab).toBeVisible();
        await this.ftaCommunicationTab.click();
        break;
      }
      default: {
        //statements;
        break;
      }
    }
  }

  async startCaseCreate(jurisdiction, caseType, event): Promise<void> {
    await this.page.getByRole('link', { name: 'Create case' }).waitFor();
    await this.page.getByRole('link', { name: 'Create case' }).click();
    await this.delay(3000);
    await this.page.getByLabel('Jurisdiction').selectOption(jurisdiction);
    await this.page.getByLabel('Case type').selectOption(caseType);
    await this.page.getByLabel('Event').selectOption(event);
    await this.page.getByRole('button', { name: 'Start' }).click();
  }
}

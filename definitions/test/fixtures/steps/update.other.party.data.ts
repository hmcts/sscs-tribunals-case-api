import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials, featureFlags } from '../../config/config';
import addUpdateOtherPartyData from '../../pages/content/update.other.party.data_en.json';
import addUpdateSubscriptionData from '../../pages/content/update.subscription.data_en.json';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import { StringUtilsComponent } from '../../utils/StringUtilsComponent';

const eventTestData = require('../../pages/content/event.name.event.description_en.json');

export class UpdateOtherPartyData extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  private async getChildSupportEndState() {
    return featureFlags.cmOtherPartyConfidentialityEnabled
      ? 'Await Other Party Data'
      : 'With FTA';
  }

  private async waitForSummaryState(expectedState: string, timeoutMs: number = 60000) {
    const deadline = Date.now() + timeoutMs;

    while (Date.now() < deadline) {
      await this.homePage.navigateToTab('Summary');
      const summaryState = await this.page
        .locator('#summaryState')
        .textContent()
        .catch(() => '');

      if (summaryState?.includes(expectedState)) {
        return;
      }

      await this.homePage.reloadPage();
    }

    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPageSectionByKeyValue(
      'Appeal status',
      expectedState
    );
  }

  private async navigateToConfidentialityTab() {
    const confidentialityTab = this.page.getByRole('tab', {
      name: /^Confidentiality$/
    });

    if (await confidentialityTab.isVisible().catch(() => false)) {
      await confidentialityTab.click();
      await expect(this.confidentialityStatusHeader()).toBeVisible();
      return;
    }

    const caseUrl = this.page.url().split('#')[0];
    await this.page.goto(`${caseUrl}#Confidentiality`);
    await expect(this.confidentialityStatusHeader()).toBeVisible();
  }

  private async getConfidentialityRowCells(rowIndex: number) {
    const table = this.page
      .locator('table:visible')
      .filter({
        has: this.confidentialityStatusHeader()
      })
      .last();
    const row = table.locator('tr').nth(rowIndex + 1);
    await expect(row).toBeVisible();
    const cellTexts = await row.locator('td').allTextContents();
    return cellTexts.map((text) => text.replace(/\s+/g, ' ').trim());
  }

  private confidentialityStatusHeader() {
    return this.page.locator('th').filter(
      {hasText: /^Confidentiality Status$/}
    );
  }

  private getTodayLondonDate() {
    return new Intl.DateTimeFormat('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      timeZone: 'Europe/London'
    }).format(new Date());
  }

  private async verifyConfidentialityRows(rows: Array<{
    party: string;
    name: string;
    status: string;
    confirmed?: 'blank' | 'today';
  }>) {
    await this.navigateToConfidentialityTab();

    const todayDate = this.getTodayLondonDate().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const todayPattern = new RegExp(
      `^${todayDate}, \\d{1,2}:\\d{2}(?::\\d{2})? [ap]m$`,
      'i'
    );

    for (let i = 0; i < rows.length; i++) {
      const cells = await this.getConfidentialityRowCells(i);
      expect(cells[0]).toBe(rows[i].party);
      expect(cells[1]).toBe(rows[i].name);
      expect(cells[2]).toBe(rows[i].status);

      if (rows[i].confirmed === 'blank') {
        expect(cells[3] || '').toBe('');
      } else if (rows[i].confirmed === 'today') {
        expect(cells[3] || '').toMatch(todayPattern);
      }
    }
  }

  private async submitEventWithOptionalSecondSubmit(attempts: number = 5) {
    let lastError: unknown;

    for (let attempt = 1; attempt <= attempts; attempt++) {
      try {
        await this.page.getByRole('button', { name: 'Submit', exact: true }).click();
        await this.page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});

        const secondSubmit = this.page.getByRole('button', {
          name: 'Submit',
          exact: true
        });

        if (await secondSubmit.isVisible().catch(() => false)) {
          await secondSubmit.click();
          await this.page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
        }

        const concurrencyErrorVisible = await this.page
          .getByRole('heading', { name: 'The event could not be created' })
          .isVisible()
          .catch(() => false);

        if (!concurrencyErrorVisible) {
          return;
        }

        lastError = new Error('The event could not be created');
        if (attempt === attempts) {
          throw lastError;
        }

        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab).toBeVisible();
      } catch (error) {
        lastError = error;
        if (attempt === attempts) {
          throw lastError;
        }
        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab).toBeVisible();
      }
    }

    throw lastError;
  }

  private async reloginIfRedirectedToSignIn(user, caseId: string) {
    const onSignInPage =
      (await this.page.locator('#username').isVisible().catch(() => false)) ||
      (await this.page
        .getByRole('heading', { name: 'Sign in or create an account' })
        .isVisible()
        .catch(() => false));

    if (onSignInPage) {
      await this.loginUserWithCaseId(user, true, caseId);
    }
  }

  /*
    ToDo: performOtherPartyData function is replicated for each benefit type. There are a few mandatory fields and few different
    fields on update other party data page for different benefit types. The functions below (perform<xyz>) and the corresponding
    page functiona (apply<xyz) are duplicated for each benefit type. These need to be refactored to have single functions regardless
    of benefit type - Rohith - 07/02/2025
  */

    async performUpdateOtherPartyData(caseId: string) {
    // Creating case - CHILDSUPPORT
    const ChildSupportCaseId = await createCaseBasedOnCaseType('CHILDSUPPORT');

    // Starting event
    await this.goToUpdateOtherPartyData(ChildSupportCaseId);
    await this.updateOtherPartyDataPage.verifyPageContent();

    // Filling fields and Submitting the event
    await this.updateOtherPartyDataPage.applyOtherPartyData();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    // Adding other party subscription
    await this.homePage.chooseEvent('Update subscription');
    await this.updateOtherPartyDataPage.applyOtherPartiesSubscription();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    // Verifying History tab + end state
    await this.verifyHistoryTabDetails('Update subscription');
    await this.historyTab.verifyPageContentByKeyValue(
      'End state',
      await this.getChildSupportEndState()
    );
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Update subscription'
    );

    // Navigate to Other Party Details tab + validations
    await this.verifyOtherPartyDetails('Other parties 1');
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'First Name',
      addUpdateOtherPartyData.updateOtherPartyDataFirstName
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Last Name',
      addUpdateOtherPartyData.updateOtherPartyDataLastName
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Address Line 1',
      addUpdateOtherPartyData.updateOtherPartyDataAddressLine
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Town',
      addUpdateOtherPartyData.updateOtherPartyDataAddressTown
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Postcode',
      addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode
    );
    await expect(
      this.page.locator(
        `//tr[.//th[normalize-space()="Unacceptable Customer Behaviour"] and .//td[normalize-space()="${addUpdateOtherPartyData.updateOtherPartyDataBehaviour}"]]`
      ).first()
    ).toBeVisible();
    await this.page
      .getByRole('row', { name: 'Confidentiality Required No', exact: true })
      .locator(`//tr[.='Confidentiality RequiredNo']`);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Role',
      addUpdateOtherPartyData.updateOtherPartyDataRole
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Track Your Appeal Number',
      addUpdateSubscriptionData.updateSubscriptionTrackYAotherParty
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Email Address',
      addUpdateSubscriptionData.updateSubscriptionEmailotherParty
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Mobile Number',
      addUpdateSubscriptionData.updateSubscriptionMobileNumberotherParty
    );
  }

  async performUpdateOtherPartyDataTaxCredit(caseId: string) {
    // Creating case - TAX CREDIT
    const TaxCreditCaseId = await createCaseBasedOnCaseType('TAX CREDIT');

    // Starting event
    await this.goToUpdateOtherPartyData(TaxCreditCaseId);
    await this.updateOtherPartyDataPage.verifyPageContent();

    // Filling fields and Submitting the event
    await this.updateOtherPartyDataPage.applyOtherPartyDataTaxCredit();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    // Adding other party subscription
    await this.homePage.chooseEvent('Update subscription');
    await this.updateOtherPartyDataPage.applyOtherPartiesSubscription();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    // Verifying History tab + end state
    await this.verifyHistoryTabDetails('Update subscription');
    await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Update subscription'
    );

    // Navigate to Other Party Details tab + validations
    await this.verifyOtherPartyDetails('Other parties');
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'First Name',
      addUpdateOtherPartyData.updateOtherPartyDataFirstName
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Last Name',
      addUpdateOtherPartyData.updateOtherPartyDataLastName
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Address Line 1',
      addUpdateOtherPartyData.updateOtherPartyDataAddressLine
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Town',
      addUpdateOtherPartyData.updateOtherPartyDataAddressTown
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Postcode',
      addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode
    );
    await this.page
      .getByRole('row', { name: 'Confidentiality Required No', exact: true })
      .locator(`//tr[.='Confidentiality RequiredNo']`); // couldn't use the same method as other options for these 2 lines 106, 107
    await this.page
      .getByRole('row', {
        name: 'Unacceptable Customer Behaviour No',
        exact: true
      })
      .locator(`//span[.='Unacceptable Customer Behaviour']`);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Track Your Appeal Number',
      addUpdateSubscriptionData.updateSubscriptionTrackYAotherParty
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Email Address',
      addUpdateSubscriptionData.updateSubscriptionEmailotherParty
    );
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue(
      'Mobile Number',
      addUpdateSubscriptionData.updateSubscriptionMobileNumberotherParty
    );
  }

  async performUpdateOtherPartyDataIBC(caseId: string) {
    // await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.chooseEvent("Update other party data");
    await this.updateOtherPartyDataPage.applyOtherPartyData("IBC");
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async makeChildSupportCaseConfidential(caseId: string) {
    await this.goToUpdateOtherPartyData(caseId);
    await this.updateOtherPartyDataPage.verifyPageContent();
    await this.updateOtherPartyDataPage.applyChildSupportConfidentialOtherPartyData();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    await this.homePage.chooseEvent('Update subscription');
    await this.updateOtherPartyDataPage.applyOtherPartiesSubscription();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();
  }

  async addOtherPartyDataForAwaitOtherPartyData(caseId: string, user) {
    await this.loginUserWithCaseId(user, true, caseId);
    await this.waitForSummaryState('Await Other Party Data');

    await this.homePage.chooseEvent('Add other party data');
    await this.updateOtherPartyDataPage.verifyPageContent('Add other party data');
    await this.updateOtherPartyDataPage.applyChildSupportFtaAddOtherPartyData();
    await this.page.getByRole('button', { name: 'Submit', exact: true }).click();

    await this.waitForSummaryState('Await Confidentiality Requirements');
  }

  async completeChildSupportConfidentialityDeterminationFlow(caseId: string) {
    const appellantName = 'Tester John';
    const otherPartyName = 'Test1 Test1';

    await this.loginUserWithCaseId(credentials.dwpResponseWriter, true, caseId);
    await this.waitForSummaryState('Await Other Party Data');
    await this.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: appellantName,
        status: 'Undetermined',
        confirmed: 'blank'
      }
    ]);

    await this.homePage.chooseEvent('Add other party data');
    await this.updateOtherPartyDataPage.verifyPageContent('Add other party data');
    await this.updateOtherPartyDataPage.applyChildSupportFtaAddOtherPartyDataWithValues({
      title: 'Mr',
      firstName: 'Test1',
      lastName: 'Test1',
      addressLine1: 'Test1',
      town: 'Test1',
      postcode: 'BN19SA'
    });
    await this.submitEventWithOptionalSecondSubmit();
    await this.waitForSummaryState('Await Confidentiality Requirements');
    await this.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: appellantName,
        status: 'Undetermined',
        confirmed: 'blank'
      },
      {
        party: 'Other Party 1',
        name: otherPartyName,
        status: 'Undetermined',
        confirmed: 'blank'
      }
    ]);

    await this.homePage.signOut();

    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.reloginIfRedirectedToSignIn(credentials.amCaseWorker, caseId);
    await this.waitForSummaryState('Await Confidentiality Requirements');
    await this.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: appellantName,
        status: 'Undetermined',
        confirmed: 'blank'
      },
      {
        party: 'Other Party 1',
        name: otherPartyName,
        status: 'Undetermined',
        confirmed: 'blank'
      }
    ]);

    await this.homePage.chooseEvent('Update other party data');
    await this.page.locator('#otherParties_0_confidentialityRequired_No').click();
    await this.submitEventWithOptionalSecondSubmit();
    await this.waitForSummaryState('Await Confidentiality Requirements');
    await this.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: appellantName,
        status: 'Undetermined',
        confirmed: 'blank'
      },
      {
        party: 'Other Party 1',
        name: otherPartyName,
        status: 'No',
        confirmed: 'today'
      }
    ]);

    await this.homePage.chooseEvent('Confidentiality Confirmed');
    await this.page.getByRole('button', { name: 'Submit', exact: true }).click();
    await expect(
      this.page.getByText(
        'Confidentiality for all parties must be determined to either Yes or No.'
      )
    ).toBeVisible();
    await this.page.getByRole('button', { name: 'Cancel', exact: true }).click();

    await this.homePage.chooseEvent('Update to case data');
    await this.page
      .getByRole('textbox', { name: 'FTA Issuing Office (Optional)' })
      .fill('Child Maintenance Service Group');
    await this.page
      .getByRole('textbox', { name: /National insurance number/ })
      .fill(StringUtilsComponent.getRandomNINumber());
    await this.page
      .getByRole('group', { name: 'Confidentiality Status' })
      .getByLabel('Yes')
      .check();
    await this.page
      .getByRole('group', { name: 'Appellant Role (Optional)' })
      .getByLabel('Role (Optional)')
      .selectOption({ label: 'Paying parent' });
    await this.page
      .getByRole('textbox', { name: 'Child maintenance number' })
      .fill('123456');
    await this.submitEventWithOptionalSecondSubmit();
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPageSectionByKeyValue(
      'Confidentiality Required',
      'Yes'
    );
    await this.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: appellantName,
        status: 'Yes',
        confirmed: 'today'
      },
      {
        party: 'Other Party 1',
        name: otherPartyName,
        status: 'No',
        confirmed: 'today'
      }
    ]);

    await this.homePage.chooseEvent('Confidentiality Confirmed');
    await this.submitEventWithOptionalSecondSubmit();
    await this.reloginIfRedirectedToSignIn(credentials.amCaseWorker, caseId);
    await this.waitForSummaryState('With FTA');
    await this.homePage.navigateToTab('Appeal Details');
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue(
      'FTA State',
      'Appeal to-be registered'
    );
  }
  
  async verifyOtherPartyDetailsIBC(){
    // Navigate to Other Party Details tab + validations
    await this.homePage.navigateToTab("Other Party Details");
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Title', addUpdateOtherPartyData.updateOtherPartyDataTitle);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('First Name', addUpdateOtherPartyData.updateOtherPartyDataFirstName);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Last Name', addUpdateOtherPartyData.updateOtherPartyDataLastName);

    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Address Line 1', addUpdateOtherPartyData.updateOtherPartyDataAddressLine);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Town', addUpdateOtherPartyData.updateOtherPartyDataAddressTown);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Postcode', addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Country', addUpdateOtherPartyData.updateOtherPartyDataAddressCountry);
    await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Living in England, Scotland, Wales or Northern Ireland', addUpdateOtherPartyData.updateOtherPartyDataLivingInUK);

    await this.page.getByRole('row', { name: 'Confidentiality Required No', exact: true }).locator(`//tr[.='Confidentiality RequiredNo']`); // couldn't use the same method as other options for these 2 lines 58, 59
    await this.page.getByRole('row', { name: 'Unacceptable Customer Behaviour No', exact: true }).locator(`//span[.='Unacceptable Customer Behaviour']`);
    // await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Role', addUpdateOtherPartyData.updateOtherPartyDataRole);
    // await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Track Your Appeal Number', addUpdateSubscriptionData.updateSubscriptionTrackYAotherParty);
    // await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Email Address', addUpdateSubscriptionData.updateSubscriptionEmailotherParty);
    // await this.otherPartyDetailsTab.verifyPageContentByKeyValue('Mobile Number', addUpdateSubscriptionData.updateSubscriptionMobileNumberotherParty);
  }
  
  // Event created to select Update other party data event from next steps dropdown menu:
  private async chooseEventWithRetry(eventName: string, attempts: number = 5) {
    let lastError;
    for (let attempt = 1; attempt <= attempts; attempt++) {
      try {
        await this.homePage.chooseEvent(eventName);
        return;
      } catch (error) {
        lastError = error;
        if (attempt === attempts) {
          throw lastError;
        }
        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab.first()).toBeVisible();
      }
    }
  }

  private async goToUpdateOtherPartyData(
    caseId: string
  ) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.chooseEventWithRetry('Update other party data');
  }
}

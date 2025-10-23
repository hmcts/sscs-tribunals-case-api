import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import addUpdateOtherPartyData from '../../pages/content/update.other.party.data_en.json';
import addUpdateSubscriptionData from '../../pages/content/update.subscription.data_en.json';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';

const eventTestData = require('../../pages/content/event.name.event.description_en.json');

export class UpdateOtherPartyData extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
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
    await this.goToUpdateOtherPartyData(this.page, ChildSupportCaseId);
    await this.updateOtherPartyDataPage.verifyPageContent();

    // Filling fields and Submitting the event
    await this.updateOtherPartyDataPage.applyOtherPartyData();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();
    await this.homePage.delay(3000);

    // Adding other party subscription
    await this.homePage.chooseEvent('Update subscription');
    await this.updateOtherPartyDataPage.applyOtherPartiesSubscription();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();
    await this.homePage.delay(3000);

    // Verifying History tab + end state
    await this.verifyHistoryTabDetails('Update subscription');
    await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
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
    await this.page
      .getByRole('row', { name: 'Confidentiality Required No', exact: true })
      .locator(`//tr[.='Confidentiality RequiredNo']`); // couldn't use the same method as other options for these 2 lines 58, 59
    await this.page
      .getByRole('row', {
        name: 'Unacceptable Customer Behaviour No',
        exact: true
      })
      .locator(`//span[.='Unacceptable Customer Behaviour']`);
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
    await this.goToUpdateOtherPartyData(this.page, TaxCreditCaseId);
    await this.updateOtherPartyDataPage.verifyPageContent();

    // Filling fields and Submitting the event
    await this.updateOtherPartyDataPage.applyOtherPartyDataTaxCredit();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();
    await this.homePage.delay(3000);

    // Adding other party subscription
    await this.homePage.chooseEvent('Update subscription');
    await this.updateOtherPartyDataPage.applyOtherPartiesSubscription();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();
    await this.homePage.delay(3000);

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
  private async goToUpdateOtherPartyData(page: Page, caseId: string) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.chooseEvent('Update other party data');
  }
}

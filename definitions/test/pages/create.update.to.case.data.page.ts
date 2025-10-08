import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import dateUtilsComponent from '../utils/DateUtilsComponent';


let webAction: WebAction;

export class CreateUpdateToCaseDataPage {

  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webAction = new WebAction(this.page);
  }

  async fillFormData(formData: any, journey: string): Promise<void> {
    await this.selectChannelOfReceipt(formData.channelType);
    await this.selectBenefitType(formData.benefitType);
    await this.selectHearingType(formData.hearingType);
    await this.enterMrnOrReviewDecisionNoticeDetails(formData.mrnOrReviewDecisionNoticeDetails);
    await this.enterAppellantDetails(formData.appellantDetails);
    await this.selectHearingSubtype(formData.hearingSubtype);
    await this.enterHearingOptions(formData.hearingOptions);
    await this.enterAppealReasons(formData.appealReasons);
    await this.enterRepresentativeDetails(formData.representativeDetails);
    if (journey === 'create') {
      await this.isScottishCase(formData.isScottishCase);
      await this.enterCaseCreatedDate();
    }
    await this.enterRegionalCentre(formData.regionalCentre, journey);
    await this.page.getByRole('button', { name: 'Submit' }).scrollIntoViewIfNeeded();
    await this.page.getByRole('button', { name: 'Submit' }).waitFor();
    await webAction.clickButton('Submit');
  }

  async selectChannelOfReceipt(channelType): Promise<void> {
    await this.page.locator('#appeal_receivedVia').selectOption({ label: channelType });
  }

  async selectBenefitType(benefitType): Promise<void> {
    await this.page.locator('#appeal_benefitType_descriptionSelection').selectOption(benefitType);
  }

  async selectHearingType(hearingType): Promise<void> {
    await this.page.locator('#appeal_hearingType').selectOption(hearingType);
  }

  async enterMrnOrReviewDecisionNoticeDetails(mrnOrReviewDecisionNoticeDetails): Promise<void> {
    const currDate = new Date();
    currDate.setDate(new Date().getDate());
    let formattedDueDate = dateUtilsComponent.formatDateToYYYYMMDD(currDate);

    await this.page.getByRole('group', { name: 'MRN/Review Decision Notice Date (Optional)' }).getByLabel('Day').fill(formattedDueDate.split('-')[2]);
    await this.page.getByRole('group', { name: 'MRN/Review Decision Notice Date (Optional)' }).getByLabel('Month').fill(formattedDueDate.split('-')[1]);
    await this.page.getByRole('group', { name: 'MRN/Review Decision Notice Date (Optional)' }).getByLabel('Year').fill(formattedDueDate.split('-')[0]);

    await this.page.getByLabel('MRN/Review Decision Notice Late Reason (Optional)').fill(mrnOrReviewDecisionNoticeDetails.lateReason);
  }

  async enterAppellantDetails(appellantData): Promise<void> {
    await this.page.locator('#appeal_appellant_name_title').fill(appellantData.nameDetails.title);
    await this.page.locator('#appeal_appellant_name_firstName').fill(appellantData.nameDetails.firstName);
    await this.page.locator('#appeal_appellant_name_middleName').fill(appellantData.nameDetails.middleName);
    await this.page.locator('#appeal_appellant_name_lastName').fill(appellantData.nameDetails.lastName);

    await this.page.getByRole('textbox', { name: 'Day Day Day' }).fill(appellantData.identityDetails.dob.split('-')[0]);
    await this.page.getByRole('textbox', { name: 'Month Month Month' }).fill(appellantData.identityDetails.dob.split('-')[1]);
    await this.page.getByRole('textbox', { name: 'Year Year Year' }).fill(appellantData.identityDetails.dob.split('-')[2]);
    await this.page.locator('#appeal_appellant_identity_ibcaReference').fill(appellantData.identityDetails.ibcaReferenceNumber);

    await this.page.locator('#appeal_appellant_address_line1').fill(appellantData.addressDetails.line1);
    await this.page.locator('#appeal_appellant_address_line2').fill(appellantData.addressDetails.line2);
    await this.page.locator('#appeal_appellant_address_town').fill(appellantData.addressDetails.town);
    await this.page.locator('#appeal_appellant_address_county').fill(appellantData.addressDetails.county);
    await this.page.locator('#appeal_appellant_address_postcode').fill(appellantData.addressDetails.postcode);
    await this.page.locator('#appeal_appellant_address_country').fill(appellantData.addressDetails.country);
    await this.page.getByRole('group', { name: 'Does the appellant live in' }).getByLabel(appellantData.addressDetails.isLiveInEngland).check();

    await this.page.locator('#appeal_appellant_contact_phone').fill(appellantData.contactDetails.contactNumber);
    await this.page.locator('#appeal_appellant_contact_mobile').fill(appellantData.contactDetails.mobileNumber);
    await this.page.locator('#appeal_appellant_contact_email').fill(appellantData.contactDetails.contactEmail);

    await this.page.locator('#appeal_appellant_ibcRole').last().selectOption(appellantData.appellantRole);
  }

  async selectHearingSubtype(hearingSubtype): Promise<void> {
    await this.page.getByRole('group', { name: 'Wants hearing type telephone' }).getByLabel(hearingSubtype.telephone).check();
    await this.page.getByRole('group', { name: 'Wants hearing type video (' }).getByLabel(hearingSubtype.video).check();
    await this.page.getByRole('group', { name: 'Wants hearing type face to' }).getByLabel(hearingSubtype.faceToFace).check();
  }

  async enterHearingOptions(hearingOptions): Promise<void> {
    await this.page.getByRole('group', { name: 'Wants To Attend (Optional)' }).getByLabel(hearingOptions.isAttending).check();

    if (hearingOptions.isAttending === 'Yes') {
      await this.page.getByRole('group', { name: 'Wants Support (Optional)' }).getByLabel(hearingOptions.wantsSupport).check();
      await this.page.getByRole('group', { name: 'Unavailable dates (Optional)' }).getByLabel(hearingOptions.unavailableDates).check();

      if (hearingOptions.wantsSupport === 'Yes') {
        await this.page.getByLabel('Sign language interpreter').check();
        await this.page.getByLabel('Hearing loop').check();
        await this.page.getByLabel('Disabled access').check();
      }
      await this.page.getByRole('group', { name: 'Language Interpreter (' }).getByLabel(hearingOptions.needLanguageIntepreter).check();
      await this.page.getByLabel('Other Information (Optional)').fill(hearingOptions.otherInformation);
      await this.page.getByLabel('Sign languages (Optional)').fill(hearingOptions.signLanguages);
    }

  }

  async enterAppealReasons(appealReasons): Promise<void> {
    await this.page.getByRole('button', { name: 'Add new' }).first().click();
    await this.page.getByLabel('Reason (Optional)', { exact: true }).first().fill(appealReasons[0].reason);
    await this.page.getByRole('textbox', { name: 'Description (Optional)' }).first().fill(appealReasons[0].description);
  }

  async enterRepresentativeDetails(representativeDetails): Promise<void> {
    await this.page.getByRole('group', { name: 'Has Representative (Optional)' }).getByLabel(representativeDetails.hasRepresentative).check();
  }

  async enterSigner(signer): Promise<void> {
    await this.page.getByLabel('Signer (Optional)').fill(signer);
  }

  async enterCaseCreatedDate(): Promise<void> {
    const currDate = new Date();
    currDate.setDate(new Date().getDate());
    let formattedDueDate = dateUtilsComponent.formatDateToYYYYMMDD(currDate);

    await this.page.getByRole('group', { name: 'Case Created Date (Optional)' }).getByLabel('Day').fill(formattedDueDate.split('-')[2]);
    await this.page.getByRole('group', { name: 'Case Created Date (Optional)' }).getByLabel('Month').fill(formattedDueDate.split('-')[1]);
    await this.page.getByRole('group', { name: 'Case Created Date (Optional)' }).getByLabel('Year').fill(formattedDueDate.split('-')[0]);
  }

  async enterSCCaseNumber(): Promise<void> {

  }

  async enterRegionalCentre(regionalCentre, journey): Promise<void> {
    await this.page.getByLabel('Regional Centre (Optional)').fill(regionalCentre);
    if (journey === 'update') {
      await this.page.locator('#regionalProcessingCenter_hearingRoute').last().selectOption('List Assist'); //Bug. By default it gets configured as gap
    }
  }

  async isScottishCase(scottishCase): Promise<void> {
    await this.page.getByRole('group', { name: 'Is a Scottish Case? (Optional)' }).getByLabel(scottishCase).check();
  }

}

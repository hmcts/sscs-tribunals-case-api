import { BaseStep } from "./base";
import { Page } from "@playwright/test";

export class CreateUpdateToCaseDataSteps extends BaseStep {

  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async createIBCcase(ibcCaseData: any) {
    await this.homePage.startCaseCreate('SSCS', 'Benefit', 'incompleteApplicationReceived');
    await this.homePage.delay(3000);
    await this.createUpdateToCaseDataPage.fillFormData(ibcCaseData, 'create');
    await this.eventNameAndDescriptionPage.inputData('test reason', 'test description');
    await this.eventNameAndDescriptionPage.confirmSubmission();
    let caseId = this.page.url().match(/\/cases\/case-details\/(\d+)/)[1];
    return caseId;
  }

  async updateToCaseDataEvent(caseId, updateData) {
    await this.goToUpdateToCaseDataPage(caseId);
    await this.createUpdateToCaseDataPage.fillFormData(updateData, 'update');
    await this.eventNameAndDescriptionPage.inputData('test reason', 'test description');
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async verifySummaryTab(updateData) {
    await this.homePage.delay(3000);
    await this.homePage.navigateToTab("Summary");
    await this.summaryTab.verifyPageSectionByKeyValue("Name", updateData.appellantDetails.nameDetails.firstName);
    await this.summaryTab.verifyPageSectionByKeyValue("Address", updateData.appellantDetails.addressDetails.line1);
    await this.summaryTab.verifyPageSectionByKeyValue("Appeal type", updateData.benefitType.split("/")[0].trim());
    await this.summaryTab.verifyPageSectionByKeyValue("IBCA Reference", updateData.appellantDetails.identityDetails.ibcaReferenceNumber);
    await this.summaryTab.verifyPageSectionByKeyValue("Hearing type", updateData.hearingType);
    // await this.summaryTab.verifyPageContentByKeyValue("Regional Centre", updateData.regionalCentre);
    await this.summaryTab.verifyPageContentByKeyValue("Is a Scottish Case?", updateData.isScottishCase);
    //await this.notListablePage.verifyPageContent(); //Verifying Heading and Caption for event
  }

  async verifyAppealDetailsTab(updateData) {
    await this.homePage.navigateToTab("Appeal Details")

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Channel of receipt", updateData.channelType);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("FTA Issuing Office", updateData.ftaIssuingOffice);
    // ToDo await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("MRN/Review Decision Notice Date", updateData.mrnOrReviewDecisionNoticeDetails.date);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("MRN/Review Decision Notice Late Reason", updateData.mrnOrReviewDecisionNoticeDetails.lateReason);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Title", updateData.appellantDetails.nameDetails.title);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("First Name", updateData.appellantDetails.nameDetails.firstName);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Last Name", updateData.appellantDetails.nameDetails.lastName);

    // ToDo await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Date of birth", updateData.appellantDetails.identityDetails.date);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("IBCA Reference Number", updateData.appellantDetails.identityDetails.ibcaReferenceNumber);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Address Line 1", updateData.appellantDetails.addressDetails.line1);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Address Line 2", updateData.appellantDetails.addressDetails.line2);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Town", updateData.appellantDetails.addressDetails.town);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("County", updateData.appellantDetails.addressDetails.county);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Postcode", updateData.appellantDetails.addressDetails.postcode);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Country", updateData.appellantDetails.addressDetails.country);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Living in England, Scotland, Wales or Northern Ireland", updateData.appellantDetails.addressDetails.isLiveInEngland);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Contact Number", updateData.appellantDetails.contactDetails.contactNumber);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Mobile Number", updateData.appellantDetails.contactDetails.mobileNumber);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Contact Email", updateData.appellantDetails.contactDetails.contactEmail);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Role", updateData.appellantDetails.appellantRole);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Hearing Type", updateData.hearingType);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Wants hearing type telephone", updateData.hearingSubtype.telephone);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Wants hearing type video", updateData.hearingSubtype.video);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Wants hearing type face to face", updateData.hearingSubtype.faceToFace);

    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Wants To Attend", updateData.hearingOptions.isAttending);
    // ToDo await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Wants Support", updateData.hearingOptions.isAttending);
    // await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Language Interpreter", updateData.hearingOptions.needLanguageIntepreter);
    // await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Languages", updateData.hearingOptions.signLanguages);
    await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue("Hearing Route", updateData.hearingRoute);

  }

  private async goToUpdateToCaseDataPage(caseId: string) {
    await this.homePage.findAndNavigateToCase(caseId);
    await this.homePage.chooseEvent("Update to case data");
  }

}

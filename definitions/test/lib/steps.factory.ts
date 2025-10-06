import { test as stepsFactory } from '@playwright/test';
import { Note } from '../fixtures/steps/note';
import { ConfirmCaseLapsed } from '../fixtures/steps/confirm.case.lapsed';
import { EvidenceReminder } from '../fixtures/steps/evidence.reminder';
import { AssociateCase } from '../fixtures/steps/associate-case';
import { SendToAdmin } from '../fixtures/steps/send.to.admin';
import { InformationReceived } from '../fixtures/steps/information.received';
import { SendToJudge } from '../fixtures/steps/send.to.judge';
import { UploadResponse } from '../fixtures/steps/upload.response';
import { ListingError } from '../fixtures/steps/listing.error';
import { SendToDormant } from '../fixtures/steps/send.to.dormant';
import { VoidCase } from '../fixtures/steps/void.case';
import { StrikeOutCase } from '../fixtures/steps/strike.out.case';
import { SendToFTA } from '../fixtures/steps/send.to.fta';
import { ReadyToList } from '../fixtures/steps/ready.to.list';
import { AppealWithdrawn } from '../fixtures/steps/appeal.withdrawn';
import { RequestTimeExtension } from '../fixtures/steps/request.time.extension';
import { CreateBundle } from '../fixtures/steps/create.bundle';
import { UrgentHearing } from '../fixtures/steps/urgent.hearing';
import { RequestInfoFromParty } from '../fixtures/steps/request.info.from.party';
import { Reinstatement } from '../fixtures/steps/reinstatement';
import { AppealDormant } from '../fixtures/steps/appeal.dormant';
import { ProvideAppointeeDetails } from '../fixtures/steps/provide.appointee.details';
import { UploadHearing } from '../fixtures/steps/upload.hearing';
import { DeathOfAnAppelant } from '../fixtures/steps/death.of.an.appelant';
import { LinkCase } from '../fixtures/steps/link-case';
import { SupplementaryResponse } from '../fixtures/steps/supplementary.response';
import { UploadDocumentFurtherEvidence } from '../fixtures/steps/upload.document.further.evidence';
import { UpdateLanguagePreference } from '../fixtures/steps/update.language.preference';
import { ReviewPHE } from '../fixtures/steps/review.phe';
import { UpdateUCB } from '../fixtures/steps/update.ucb';
import { ProcessAVEvidence } from '../fixtures/steps/process.av.evidence';
import { UpdateSubscription } from '../fixtures/steps/update.subscription';
import { UpdateOtherPartyData } from '../fixtures/steps/update.other.party.data';
import { IssueDirectionsNotice } from '../fixtures/steps/issue.directions.notice';
import { WriteFinalDecision } from '../fixtures/steps/write.final.decision';
import { UpdateNotListable } from '../fixtures/steps/update.not.listable';
import { ReissueFurtherEvidence } from '../fixtures/steps/reissue.further.evidence';
import { Postponement } from '../fixtures/steps/postponement';
import { SearchFilter } from '../fixtures/steps/search.filter';
import { Hearing } from '../fixtures/steps/hearing';
import { PrepareCaseForHearing } from '../fixtures/steps/prepare.case.for.hearing';
import { EnhancedConfidentiality } from '../fixtures/steps/enhanced.confidentiality';
import { SendToInterloc } from '../fixtures/steps/send.to.interloc';
import { ReferredByAdmin } from '../fixtures/steps/referred.by.admin';
import { SendCaseToTcw } from '../fixtures/steps/send.case.to.tcw';
import { ReferredByJudge } from '../fixtures/steps/referred.by.judge';
import { AccessibilitySteps } from '../fixtures/steps/accessibilitySteps';
import { CreateUpdateToCaseDataSteps } from '../fixtures/steps/update.to.case.data';
import { GenerateAppealPdfSteps } from '../fixtures/steps/generate.appeal.pdf';
import { ManageDocuments } from '../fixtures/steps/manage.documents';
import { UpdateListingRequirement } from '../fixtures/steps/update.listing.requirements';
import { CommunicateWithFta } from '../fixtures/steps/communicate-with-fta';
import { Adjournment } from '../fixtures/steps/adjournment';
import { AmendElements } from '../fixtures/steps/amend.elements';
import { ReviewIncompleteApplication } from '../fixtures/steps/work-allocation/review.incomplete.application';

type MyStepsFixtures = {
  addNoteSteps: Note;
  associateCaseSteps: AssociateCase;
  confirmCaseLapsedSteps: ConfirmCaseLapsed;
  evidenceReminderSteps: EvidenceReminder;
  informationReceivedSteps: InformationReceived;
  sendToAdminSteps: SendToAdmin;
  sendToJudgeSteps: SendToJudge;
  listingErrorSteps: ListingError;
  uploadResponseSteps: UploadResponse;
  sendToFTASteps: SendToFTA;
  sendToDormantSteps: SendToDormant;
  voidCaseSteps: VoidCase;
  appealWithdrawnSteps: AppealWithdrawn;
  strikeOutCaseSteps: StrikeOutCase;
  readyToListSteps: ReadyToList;
  requestTimeExtensionSteps: RequestTimeExtension;
  createBundleSteps: CreateBundle;
  urgentHearingSteps: UrgentHearing;
  requestInfoFromPartySteps: RequestInfoFromParty;
  reinstatementSteps: Reinstatement;
  appealDormantSteps: AppealDormant;
  deathOfAppellant: DeathOfAnAppelant;
  linkACaseSteps: LinkCase;
  provideAppointeeDetailsSteps: ProvideAppointeeDetails;
  uploadHearingSteps: UploadHearing;
  supplementaryResponseSteps: SupplementaryResponse;
  uploadDocumentFurtherEvidenceSteps: UploadDocumentFurtherEvidence;
  updateLanguagePreferenceSteps: UpdateLanguagePreference;
  reviewPHESteps: ReviewPHE;
  issueDirectionsNoticeSteps: IssueDirectionsNotice;
  updateUCBSteps: UpdateUCB;
  updateSubscriptionSteps: UpdateSubscription;
  processAVEvidenceSteps: ProcessAVEvidence;
  updateOtherPartyDataSteps: UpdateOtherPartyData;
  issueFinalDecisionSteps: WriteFinalDecision;
  updateNotListableSteps: UpdateNotListable;
  manageDocumentsSteps: ManageDocuments;
  searchFilterSteps: SearchFilter;
  hearingSteps: Hearing;
  reissueFurtherEvidenceSteps: ReissueFurtherEvidence;
  postponementSteps: Postponement;
  prepareCaseForHearingSteps: PrepareCaseForHearing;
  enhancedConfidentialitySteps: EnhancedConfidentiality;
  sendToInterlocSteps: SendToInterloc;
  referredByAdminSteps: ReferredByAdmin;
  sendCaseToTcwSteps: SendCaseToTcw;
  referredByJudgeSteps: ReferredByJudge;
  accessibilitySteps: AccessibilitySteps;
  createUpdateToCaseDataSteps: CreateUpdateToCaseDataSteps;
  generateAppealPdfSteps: GenerateAppealPdfSteps;
  updateListingRequirementSteps: UpdateListingRequirement;
  communicateWithFtaSteps: CommunicateWithFta;
  adjournmentSteps: Adjournment;
  amendElementSteps: AmendElements;
  reviewIncompleteApplicationSteps: ReviewIncompleteApplication;
};

export const test = stepsFactory.extend<MyStepsFixtures>({
  addNoteSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const addNoteSteps = new Note(page);
    await use(addNoteSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  associateCaseSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const associateCaseSteps = new AssociateCase(page);
    await use(associateCaseSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  confirmCaseLapsedSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const confirmCaseLapsedSteps = new ConfirmCaseLapsed(page);
    await use(confirmCaseLapsedSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  evidenceReminderSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const evidenceReminderSteps = new EvidenceReminder(page);
    await use(evidenceReminderSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  informationReceivedSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const informationReceivedSteps = new InformationReceived(page);
    await use(informationReceivedSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  sendToAdminSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const sendToAdminSteps = new SendToAdmin(page);
    await use(sendToAdminSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  readyToListSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const readyToListSteps = new ReadyToList(page);
    await use(readyToListSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  sendToJudgeSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const sendToJudgeSteps = new SendToJudge(page);
    await use(sendToJudgeSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  uploadResponseSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const uploadResponseSteps = new UploadResponse(page);
    await use(uploadResponseSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  sendToFTASteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const sendToFTASteps = new SendToFTA(page);
    await use(sendToFTASteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  sendToDormantSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const sendToDormantSteps = new SendToDormant(page);
    await use(sendToDormantSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  voidCaseSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const voidCaseSteps = new VoidCase(page);
    await use(voidCaseSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  strikeOutCaseSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const strikeOutCaseSteps = new StrikeOutCase(page);
    await use(strikeOutCaseSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  listingErrorSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const listingErrorSteps = new ListingError(page);
    await use(listingErrorSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  appealWithdrawnSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const appealWithdrawnSteps = new AppealWithdrawn(page);
    await use(appealWithdrawnSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  requestTimeExtensionSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const requestTimeExtensionSteps = new RequestTimeExtension(page);
    await use(requestTimeExtensionSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  createBundleSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const createBundleSteps = new CreateBundle(page);
    await use(createBundleSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  urgentHearingSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const urgentHearingSteps = new UrgentHearing(page);
    await use(urgentHearingSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  issueDirectionsNoticeSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const issueDirectionsNoticeSteps = new IssueDirectionsNotice(page);
    await use(issueDirectionsNoticeSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  reinstatementSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const reinstatementSteps = new Reinstatement(page);
    await use(reinstatementSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  appealDormantSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const appealDormantSteps = new AppealDormant(page);
    await use(appealDormantSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  deathOfAppellant: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const deathOfAppellantSteps = new DeathOfAnAppelant(page);
    await use(deathOfAppellantSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  linkACaseSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const linkACaseSteps = new LinkCase(page);
    await use(linkACaseSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  provideAppointeeDetailsSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const provideAppointeeDetailsSteps = new ProvideAppointeeDetails(page);
    await use(provideAppointeeDetailsSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  uploadHearingSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const uploadHearingSteps = new UploadHearing(page);
    await use(uploadHearingSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  requestInfoFromPartySteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const requestInfoFromPartySteps = new RequestInfoFromParty(page);
    await use(requestInfoFromPartySteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  supplementaryResponseSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const supplementaryResponseSteps = new SupplementaryResponse(page);
    await use(supplementaryResponseSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  uploadDocumentFurtherEvidenceSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const uploadDocumentFurtherEvidenceSteps =
      new UploadDocumentFurtherEvidence(page);
    await use(uploadDocumentFurtherEvidenceSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  updateLanguagePreferenceSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const updateLanguagePreferenceSteps = new UpdateLanguagePreference(page);
    await use(updateLanguagePreferenceSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  reviewPHESteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const reviewPHESteps = new ReviewPHE(page);
    await use(reviewPHESteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  updateUCBSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const updateUCBSteps = new UpdateUCB(page);
    await use(updateUCBSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  updateSubscriptionSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const updateSubscriptionSteps = new UpdateSubscription(page);
    await use(updateSubscriptionSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  issueFinalDecisionSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const issueFinalDecisionSteps = new WriteFinalDecision(page);
    await use(issueFinalDecisionSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  processAVEvidenceSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const processAVEvidenceSteps = new ProcessAVEvidence(page);
    await use(processAVEvidenceSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  updateOtherPartyDataSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const updateOtherPartyDataSteps = new UpdateOtherPartyData(page);
    await use(updateOtherPartyDataSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  updateNotListableSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const updateNotListableSteps = new UpdateNotListable(page);
    await use(updateNotListableSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  manageDocumentsSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const manageDocumentsSteps = new ManageDocuments(page);
    await use(manageDocumentsSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  searchFilterSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const searchFilterSteps = new SearchFilter(page);
    await use(searchFilterSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  reissueFurtherEvidenceSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const reissueFurtherEvidenceSteps = new ReissueFurtherEvidence(page);
    await use(reissueFurtherEvidenceSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  postponementSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const postponementSteps = new Postponement(page);
    await use(postponementSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  hearingSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const hearingSteps = new Hearing(page);
    await use(hearingSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  prepareCaseForHearingSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const prepareCaseForHearingSteps = new PrepareCaseForHearing(page);
    await use(prepareCaseForHearingSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  enhancedConfidentialitySteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const enhancedConfidentialitySteps = new EnhancedConfidentiality(page);
    await use(enhancedConfidentialitySteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  sendToInterlocSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const sendToInterlocSteps = new SendToInterloc(page);
    await use(sendToInterlocSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  referredByAdminSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const ReferredByAdminSteps = new ReferredByAdmin(page);
    await use(ReferredByAdminSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  sendCaseToTcwSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const sendCaseToTcwSteps = new SendCaseToTcw(page);
    await use(sendCaseToTcwSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  referredByJudgeSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const ReferredByJudgeSteps = new ReferredByJudge(page);
    await use(ReferredByJudgeSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  accessibilitySteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const accessibilitySteps = new AccessibilitySteps(page);
    await use(accessibilitySteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  createUpdateToCaseDataSteps: async ({ page }, use) => {
    const createUpdateToCaseDataSteps = new CreateUpdateToCaseDataSteps(page);
    await use(createUpdateToCaseDataSteps);
  },
  generateAppealPdfSteps: async ({ page }, use) => {
    const generateAppealPdfSteps = new GenerateAppealPdfSteps(page);
    await use(generateAppealPdfSteps);
  },
  updateListingRequirementSteps: async ({ page }, use) => {
    const updateListingRequirementSteps = new UpdateListingRequirement(page);
    await use(updateListingRequirementSteps);
  },
  communicateWithFtaSteps: async ({ page }, use, testInfo) => {
    console.log(`Test started: ${testInfo.title}`);
    const communicateWithFtaSteps = new CommunicateWithFta(page);
    await use(communicateWithFtaSteps);
    console.log(`${testInfo.title} ${testInfo.status}`);
  },
  adjournmentSteps: async ({ page }, use) => {
    const adjournmentSteps = new Adjournment(page);
    await use(adjournmentSteps);
  },
  amendElementSteps: async ({ page }, use) => {
    const amendElementSteps = new AmendElements(page);
    await use(amendElementSteps);
  },
  reviewIncompleteApplicationSteps: async ({ page }, use) => {
    const reviewIncompleteApplicationSteps = new ReviewIncompleteApplication(page);
    await use(reviewIncompleteApplicationSteps);
  }
});

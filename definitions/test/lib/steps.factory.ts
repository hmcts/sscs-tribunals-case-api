import {test as stepsFactory} from '@playwright/test';
import {Note} from '../fixtures/steps/note';
import {ConfirmCaseLapsed} from '../fixtures/steps/confirm.case.lapsed';
import {EvidenceReminder} from '../fixtures/steps/evidence.reminder';
import {AssociateCase} from '../fixtures/steps/associate-case';
import {SendToAdmin} from '../fixtures/steps/send.to.admin';
import {InformationReceived} from '../fixtures/steps/information.received';
import {SendToJudge} from '../fixtures/steps/send.to.judge';
import {UploadResponse} from '../fixtures/steps/upload.response';
import {ListingError} from "../fixtures/steps/listing.error";
import {SendToDormant} from '../fixtures/steps/send.to.dormant';
import {VoidCase} from '../fixtures/steps/void.case';
import {StrikeOutCase} from '../fixtures/steps/strike.out.case';
import {SendToFTA} from '../fixtures/steps/send.to.fta';
import {ReadyToList} from '../fixtures/steps/ready.to.list';
import {AppealWithdrawn} from '../fixtures/steps/appeal.withdrawn';
import {RequestTimeExtension} from '../fixtures/steps/request.time.extension';
import {CreateBundle} from '../fixtures/steps/create.bundle';
import {UrgentHearing} from '../fixtures/steps/urgent.hearing';
import {RequestInfoFromParty} from '../fixtures/steps/request.info.from.party';
import {Reinstatement} from '../fixtures/steps/reinstatement';
import {AppealDormant} from '../fixtures/steps/appeal.dormant';
import {ProvideAppointeeDetails} from '../fixtures/steps/provide.appointee.details';
import {UploadHearing} from "../fixtures/steps/upload.hearing";
import {DeathOfAnAppelant} from "../fixtures/steps/death.of.an.appelant";
import {LinkCase} from "../fixtures/steps/link-case";
import {SupplementaryResponse} from '../fixtures/steps/supplementary.response';
import {UploadDocumentFurtherEvidence} from '../fixtures/steps/upload.document.further.evidence';
import {UpdateLanguagePreference} from '../fixtures/steps/update.language.preference';
import {ReviewPHE} from '../fixtures/steps/review.phe';
import {UpdateUCB} from "../fixtures/steps/update.ucb";
import {ProcessAVEvidence} from '../fixtures/steps/process.av.evidence';
import {UpdateSubscription} from '../fixtures/steps/update.subscription'
import {UpdateOtherPartyData} from '../fixtures/steps/update.other.party.data';
import {IssueDirectionsNotice} from "../fixtures/steps/issue.directions.notice";
import {WriteFinalDecision} from "../fixtures/steps/write.final.decision";
import {UpdateNotListable} from "../fixtures/steps/update.not.listable";
import {ReissueFurtherEvidence} from '../fixtures/steps/reissue.further.evidence';
import {Postponement} from "../fixtures/steps/postponement";
import {SearchFilter} from '../fixtures/steps/search.filter';
import {Hearing} from '../fixtures/steps/hearing';
import {PrepareCaseForHearing} from '../fixtures/steps/prepare.case.for.hearing';
import {EnhancedConfidentiality} from '../fixtures/steps/enhanced.confidentiality';
import {SendToInterloc} from '../fixtures/steps/send.to.interloc';
import {ReferredByAdmin} from '../fixtures/steps/referred.by.admin';
import {SendCaseToTcw} from '../fixtures/steps/send.case.to.tcw';
import {ReferredByJudge} from '../fixtures/steps/referred.by.judge';
import {AccessibilitySteps} from "../fixtures/steps/accessibilitySteps";

type MyStepsFixtures = {
    addNoteSteps: Note
    associateCaseSteps: AssociateCase
    confirmCaseLapsedSteps: ConfirmCaseLapsed
    evidenceReminderSteps: EvidenceReminder
    informationReceivedSteps: InformationReceived
    sendToAdminSteps: SendToAdmin
    sendToJudgeSteps: SendToJudge
    listingErrorSteps: ListingError
    uploadResponseSteps: UploadResponse
    sendToFTASteps: SendToFTA
    sendToDormantSteps: SendToDormant
    voidCaseSteps: VoidCase
    appealWithdrawnSteps: AppealWithdrawn
    strikeOutCaseSteps: StrikeOutCase
    readyToListSteps: ReadyToList
    requestTimeExtensionSteps: RequestTimeExtension
    createBundleSteps: CreateBundle
    urgentHearingSteps: UrgentHearing
    requestInfoFromPartySteps: RequestInfoFromParty
    reinstatementSteps: Reinstatement
    appealDormantSteps: AppealDormant
    deathOfAppellant: DeathOfAnAppelant
    linkACaseSteps: LinkCase
    provideAppointeeDetailsSteps: ProvideAppointeeDetails
    uploadHearingSteps: UploadHearing
    supplementaryResponseSteps: SupplementaryResponse
    uploadDocumentFurtherEvidenceSteps: UploadDocumentFurtherEvidence
    updateLanguagePreferenceSteps: UpdateLanguagePreference
    reviewPHESteps: ReviewPHE
    issueDirectionsNoticeSteps: IssueDirectionsNotice
    updateUCBSteps: UpdateUCB
    updateSubscriptionSteps: UpdateSubscription
    processAVEvidenceSteps: ProcessAVEvidence
    updateOtherPartyDataSteps: UpdateOtherPartyData
    issueFinalDecisionSteps: WriteFinalDecision
    updateNotListableSteps: UpdateNotListable
    searchFilterSteps: SearchFilter
    hearingSteps: Hearing
    reissueFurtherEvidenceSteps: ReissueFurtherEvidence
    postponementSteps: Postponement
    prepareCaseForHearingSteps: PrepareCaseForHearing
    enhancedConfidentialitySteps: EnhancedConfidentiality
    sendToInterlocSteps: SendToInterloc
    referredByAdminSteps: ReferredByAdmin
    sendCaseToTcwSteps: SendCaseToTcw
    referredByJudgeSteps: ReferredByJudge
    accessibilitySteps: AccessibilitySteps
};

export const test = stepsFactory.extend<MyStepsFixtures>({
    addNoteSteps: async ({page}, use) => {
        const addNoteSteps = new Note(page);
        await use(addNoteSteps);
    },
    associateCaseSteps: async ({page}, use) => {
        const associateCaseSteps = new AssociateCase(page);
        await use(associateCaseSteps);
    },
    confirmCaseLapsedSteps: async ({page}, use) => {
        const confirmCaseLapsedSteps = new ConfirmCaseLapsed(page);
        await use(confirmCaseLapsedSteps);
    },
    evidenceReminderSteps: async ({page}, use) => {
        const evidenceReminderSteps = new EvidenceReminder(page);
        await use(evidenceReminderSteps);
    },
    informationReceivedSteps: async ({page}, use) => {
        const informationReceivedSteps = new InformationReceived(page);
        await use(informationReceivedSteps);
    },
    sendToAdminSteps: async ({page}, use) => {
        const sendToAdminSteps = new SendToAdmin(page);
        await use(sendToAdminSteps);
    },
    readyToListSteps: async ({page}, use) => {
        const readyToListSteps = new ReadyToList(page);
        await use(readyToListSteps);
    },
    sendToJudgeSteps: async ({page}, use) => {
        const sendToJudgeSteps = new SendToJudge(page);
        await use(sendToJudgeSteps);
    },
    uploadResponseSteps: async ({page}, use) => {
        const uploadResponseSteps = new UploadResponse(page);
        await use(uploadResponseSteps);
    },
    sendToFTASteps: async ({page}, use) => {
        const sendToFTASteps = new SendToFTA(page);
        await use(sendToFTASteps);
    },
    sendToDormantSteps: async ({page}, use) => {
        const sendToDormantSteps = new SendToDormant(page);
        await use(sendToDormantSteps);
    },
    voidCaseSteps: async ({page}, use) => {
        const voidCaseSteps = new VoidCase(page);
        await use(voidCaseSteps);
    },
    strikeOutCaseSteps: async ({page}, use) => {
        const strikeOutCaseSteps = new StrikeOutCase(page);
        await use(strikeOutCaseSteps);
    },
    listingErrorSteps: async ({page}, use) => {
        const listingErrorSteps = new ListingError(page);
        await use(listingErrorSteps);
    },
    appealWithdrawnSteps: async ({page}, use) => {
        const appealWithdrawnSteps = new AppealWithdrawn(page);
        await use(appealWithdrawnSteps);
    },
    requestTimeExtensionSteps: async ({page}, use) => {
        const requestTimeExtensionSteps = new RequestTimeExtension(page);
        await use(requestTimeExtensionSteps);
    },
    createBundleSteps: async ({page}, use) => {
        const createBundleSteps = new CreateBundle(page);
        await use(createBundleSteps);
    },
    urgentHearingSteps: async ({page}, use) => {
        const urgentHearingSteps = new UrgentHearing(page);
        await use(urgentHearingSteps);
    },
    issueDirectionsNoticeSteps: async ({page}, use) => {
        const issueDirectionsNoticeSteps = new IssueDirectionsNotice(page);
        await use(issueDirectionsNoticeSteps);
    },
    reinstatementSteps: async ({page}, use) => {
        const reinstatementSteps = new Reinstatement(page);
        await use(reinstatementSteps);
    },
    appealDormantSteps: async ({page}, use) => {
        const appealDormantSteps = new AppealDormant(page);
        await use(appealDormantSteps);
    },
    deathOfAppellant: async ({page}, use) => {
        const deathOfAppellantSteps = new DeathOfAnAppelant(page);
        await use(deathOfAppellantSteps);
    },
    linkACaseSteps: async ({page}, use) => {
        const linkACaseSteps = new LinkCase(page);
        await use(linkACaseSteps)
    },
    provideAppointeeDetailsSteps: async ({page}, use) => {
        const provideAppointeeDetailsSteps = new ProvideAppointeeDetails(page);
        await use(provideAppointeeDetailsSteps)
    },
    uploadHearingSteps: async ({page}, use) => {
        const uploadHearingSteps = new UploadHearing(page);
        await use(uploadHearingSteps);
    },
    requestInfoFromPartySteps: async ({page}, use) => {
        const requestInfoFromPartySteps = new RequestInfoFromParty(page);
        await use(requestInfoFromPartySteps);
    },
    supplementaryResponseSteps: async ({page}, use) => {
        const supplementaryResponseSteps = new SupplementaryResponse(page);
        await use(supplementaryResponseSteps);
    },
    uploadDocumentFurtherEvidenceSteps: async ({page}, use) => {
        const uploadDocumentFurtherEvidenceSteps = new UploadDocumentFurtherEvidence(page);
        await use(uploadDocumentFurtherEvidenceSteps);
    },
    updateLanguagePreferenceSteps: async ({page}, use) => {
        const updateLanguagePreferenceSteps = new UpdateLanguagePreference(page);
        await use(updateLanguagePreferenceSteps);
    },
    reviewPHESteps: async ({page}, use) => {
        const reviewPHESteps = new ReviewPHE(page);
        await use(reviewPHESteps);
    },
    updateUCBSteps: async ({page}, use) => {
        const updateUCBSteps = new UpdateUCB(page);
        await use(updateUCBSteps);
    },
    updateSubscriptionSteps: async ({page}, use) => {
        const updateSubscriptionSteps = new UpdateSubscription(page);
        await use(updateSubscriptionSteps);
    },
    issueFinalDecisionSteps: async ({page}, use) => {
        const issueFinalDecisionSteps = new WriteFinalDecision(page);
        await use(issueFinalDecisionSteps);
    },
    processAVEvidenceSteps: async ({page}, use) => {
        const processAVEvidenceSteps = new ProcessAVEvidence(page);
        await use(processAVEvidenceSteps);
    },
    updateOtherPartyDataSteps: async ({page}, use) => {
        const updateOtherPartyDataSteps = new UpdateOtherPartyData(page);
        await use(updateOtherPartyDataSteps);
    },
    updateNotListableSteps: async ({page}, use) => {
        const updateNotListableSteps = new UpdateNotListable(page);
        await use(updateNotListableSteps);
    },
    searchFilterSteps: async ({page}, use) => {
        const searchFilterSteps = new SearchFilter(page);
        await use(searchFilterSteps);
    },
    reissueFurtherEvidenceSteps: async ({page}, use) => {
        const reissueFurtherEvidenceSteps = new ReissueFurtherEvidence(page);
        await use(reissueFurtherEvidenceSteps);
    },
    postponementSteps: async ({page}, use) => {
        const postponementSteps = new Postponement(page);
        await use(postponementSteps);
    },
    hearingSteps: async ({page}, use) => {
        const hearingSteps = new Hearing(page);
        await use(hearingSteps);
    },
    prepareCaseForHearingSteps: async ({page}, use) => {
        const prepareCaseForHearingSteps = new PrepareCaseForHearing(page);
        await use(prepareCaseForHearingSteps);
    },
    enhancedConfidentialitySteps: async ({page}, use) => {
        const enhancedConfidentialitySteps = new EnhancedConfidentiality(page);
        await use(enhancedConfidentialitySteps);
    },
    sendToInterlocSteps: async ({page}, use) => {
        const sendToInterlocSteps = new SendToInterloc(page);
        await use(sendToInterlocSteps);
    },
    referredByAdminSteps: async ({page}, use) => {
        const ReferredByAdminSteps = new ReferredByAdmin(page);
        await use(ReferredByAdminSteps);
    },
    sendCaseToTcwSteps: async ({page}, use) => {
        const sendCaseToTcwSteps = new SendCaseToTcw(page);
        await use(sendCaseToTcwSteps);
    },
    referredByJudgeSteps: async ({page}, use) => {
        const ReferredByJudgeSteps = new ReferredByJudge(page);
        await use(ReferredByJudgeSteps);
    },
    accessibilitySteps: async ({page}, use) => {
        const accessibilitySteps = new AccessibilitySteps(page);
        await use(accessibilitySteps);
    }
})
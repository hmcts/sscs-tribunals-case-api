import {Page} from '@playwright/test';
import {HomePage} from '../../pages/common/homePage';
import {LoginPage} from '../../pages/common/loginPage';
import {UploadResponsePage} from '../../pages/upload.response.page';
import {CheckYourAnswersPage} from '../../pages/common/check.your.answers.page';
import {ResponseReviewedPage} from '../../pages/response.reviewed.page';
import {AssociateCasePage} from '../../pages/associate.case.page';
import {TextAreaPage} from "../../pages/common/text.area.page";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import { History } from "../../pages/tabs/history";
import { AppealDetails } from "../../pages/tabs/appealDetails";
import { AddNote } from '../../pages/add.note';
import { EventNameEventDescriptionPage } from '../../pages/common/event.name.event.description';
import { NotePad } from '../../pages/tabs/note.pad';
import { Summary } from "../../pages/tabs/summary";
import { Tasks } from "../../pages/tabs/tasks";
import { InformationReceivedPage } from '../../pages/information.received.page';
import { RequestTimeExtensionPage } from '../../pages/request.time.extension.page';
import { ActionFurtherEvidencePage } from '../../pages/action.further.evidence.page';
import { IssueDirectionPage } from '../../pages/issue.direction.page';
import { RequestInfoFromPartyPage } from '../../pages/request.info.from.party.page';
import { Bundles } from '../../pages/tabs/bundles';
import { CreateBundlePage } from '../../pages/create.bundle';
import {LinkCasePage} from "../../pages/link.case.page";
import { ProvideAppointeeDetailsPage } from '../../pages/provide.appointee.details.page';
import { AddHearingPage } from '../../pages/add.hearing.page';
import { HearingBookedPage } from '../../pages/hearing.booked.page';
import { UploadRecordingPage } from '../../pages/upload.recording.page';
import { HearingRecordings } from '../../pages/tabs/hearing.recordings';
import { RequestRecordingPage } from '../../pages/request.recording.page';
import { ActionRecordingPage } from '../../pages/action.recording.page';
import { Documents } from "../../pages/tabs/documents";
import { UploadDocumentPage } from "../../pages/upload.document.page";
import { RolesAndAccess } from "../../pages/tabs/roles.and.access";
import { SupplementaryResponsePage } from "../../pages/supplementary.response.page";
import { UploadDocumentFurtherEvidencePage } from '../../pages/upload.document.further.evidence.page';
import { UpdateLanguagePreferencePage } from '../../pages/update.language.preference.page';
import { Welsh } from '../../pages/tabs/welsh';
import { AmendInterlocReviewStatePage } from '../../pages/amend.interloc.review.state.page';
import { ReviewPHEPage } from '../../pages/review.phe.page';
import { ListingRequirements } from '../../pages/tabs/listing.requirements';
import { UpdateUCBPage } from '../../pages/update.ucb.page';
import { UpdateSubscriptionPage } from '../../pages/update.subscription.page';
import { Subscriptions } from '../../pages/tabs/subscriptions';
import { AudioVideoEvidence } from '../../pages/tabs/audioVideoEvidence';
import { ProcessAVPage } from '../../pages/process.av.page';
import { OtherPartyDetails } from '../../pages/tabs/other.party.details';
import { updateOtherPartyDataPage } from '../../pages/update.other.party.data.page';
import { SendCaseToTcwPage } from '../../pages/send.case.to.tcw.page';
import {WriteFinalDecisionPages} from "../../pages/write.final.decision.page";
import {SendToInterlocPrevalidPage} from "../../pages/send.to.interloc.prevalid.page";
import {NotListablePage} from "../../pages/not.listable.page";
import { UpdateNotListablePage } from "../../pages/update.not.listable.page";
import { SearchFilterPage } from '../../pages/search.filter.page';
import { Hearings } from '../../pages/tabs/hearings';
import { ListingRequirementPage } from '../../pages/listing.requirements.page';
import { ReissueFurtherEvidencePage } from '../../pages/reissue.further.evidence.page';
import { PostponementPages } from "../../pages/postponement.page";
import { PrepareCaseForHearingPage } from '../../pages/prepare.case.for.hearing.page';
import { ReviewConfidentialityPage } from '../../pages/review.confidentiality.page'
import { SendToJudgePage } from '../../pages/send.to.judge.page';


export abstract class BaseStep {

  readonly page : Page;
  protected loginPage: LoginPage;
  protected homePage: HomePage;
  protected uploadResponsePage: UploadResponsePage;
  protected checkYourAnswersPage: CheckYourAnswersPage;
  protected responseReviewedPage: ResponseReviewedPage;
  protected addNotePage: AddNote;
  protected notePadTab: NotePad;
  protected eventNameAndDescriptionPage: EventNameEventDescriptionPage;
  protected associateCasePage: AssociateCasePage;
  protected informationReceivedPage: InformationReceivedPage;
  protected textAreaPage: TextAreaPage;
  protected historyTab: History;
  protected appealDetailsTab: AppealDetails;
  protected summaryTab: Summary;
  protected tasksTab: Tasks;
  protected welshTab: Welsh;
  protected bundlesTab: Bundles;
  protected createBundlePage: CreateBundlePage;
  protected requestTimeExtensionPage: RequestTimeExtensionPage;
  protected actionFurtherEvidencePage: ActionFurtherEvidencePage;
  protected issueDirectionPage: IssueDirectionPage;
  protected requestInfoFromPartyPage: RequestInfoFromPartyPage;
  protected linkACasePage: LinkCasePage;
  protected provideAppointeeDetailsPage: ProvideAppointeeDetailsPage;
  protected addHearingPage: AddHearingPage;
  protected hearingBookedPage: HearingBookedPage;
  protected uploadRecordingPage: UploadRecordingPage;
  protected hearingRecordingsTab: HearingRecordings;
  protected requestRecordingPage: RequestRecordingPage;
  protected actionRecordingPage: ActionRecordingPage;
  protected documentsTab: Documents;
  protected uploadDocumentPage: UploadDocumentPage;
  protected rolesAndAccessTab: RolesAndAccess;
  protected supplementaryResponsePage: SupplementaryResponsePage;
  protected uploadDocumentFurtherEvidencePage: UploadDocumentFurtherEvidencePage;
  protected updateLanguagePreferencePage: UpdateLanguagePreferencePage;
  protected amendInterlocReviewStatePage: AmendInterlocReviewStatePage;
  protected reviewPHEPage: ReviewPHEPage;
  protected listingRequirementsTab: ListingRequirements;
  protected updateUCBPage: UpdateUCBPage;
  protected updateSubscriptionPage: UpdateSubscriptionPage;
  protected subscriptionsTab: Subscriptions;
  protected avTab: AudioVideoEvidence;
  protected processAVPage: ProcessAVPage;
  protected updateOtherPartyDataPage: updateOtherPartyDataPage;
  protected otherPartyDetailsTab: OtherPartyDetails;
  protected sendCaseToTcwPage: SendCaseToTcwPage;
  protected writeFinalDecisionPage : WriteFinalDecisionPages;
  protected sendToInterlocPrevalidPage : SendToInterlocPrevalidPage;
  protected sendToJudgePage: SendToJudgePage;
  protected notListablePage: NotListablePage;
  protected updateNotListablePage: UpdateNotListablePage;
  protected searchFilterPage: SearchFilterPage;
  protected hearingsTab: Hearings;
  protected listingRequirementPage: ListingRequirementPage
  protected reissueFurtherEvidencePage: ReissueFurtherEvidencePage;
  protected postponementPage : PostponementPages;
  protected prepareCaseForHearingPage: PrepareCaseForHearingPage;
  protected reviewConfidentialityPage: ReviewConfidentialityPage;

   constructor(page: Page) {
        this.page = page;
        this.loginPage = new LoginPage(this.page);
        this.homePage = new HomePage(this.page);
        this.uploadResponsePage = new UploadResponsePage(this.page);
        this.checkYourAnswersPage = new CheckYourAnswersPage(this.page);
        this.responseReviewedPage = new ResponseReviewedPage(this.page);
        this.eventNameAndDescriptionPage = new EventNameEventDescriptionPage(this.page);
        this.historyTab = new History(this.page);
        this.appealDetailsTab = new AppealDetails(this.page);
        this.addNotePage = new AddNote(this.page);
        this.notePadTab = new NotePad(this.page);
        this.associateCasePage = new AssociateCasePage(this.page);
        this.informationReceivedPage = new InformationReceivedPage(this.page);
        this.summaryTab = new Summary(this.page);
        this.textAreaPage = new TextAreaPage(this.page);
        this.tasksTab = new Tasks(this.page);
        this.welshTab = new Welsh(this.page);
        this.requestTimeExtensionPage = new RequestTimeExtensionPage(this.page);
        this.actionFurtherEvidencePage = new ActionFurtherEvidencePage(this.page);
        this.issueDirectionPage = new IssueDirectionPage(this.page);
        this.requestInfoFromPartyPage = new RequestInfoFromPartyPage(this.page);
        this.bundlesTab = new Bundles(this.page);
        this.createBundlePage = new CreateBundlePage(this.page);
        this.linkACasePage = new LinkCasePage(this.page);
        this.provideAppointeeDetailsPage = new ProvideAppointeeDetailsPage(this.page);
        this.addHearingPage = new AddHearingPage(this.page);
        this.hearingBookedPage = new HearingBookedPage(this.page);
        this.uploadRecordingPage = new UploadRecordingPage(this.page);
        this.hearingRecordingsTab = new HearingRecordings(this.page);
        this.requestRecordingPage = new RequestRecordingPage(this.page);
        this.actionRecordingPage = new ActionRecordingPage(this.page);
        this.documentsTab = new Documents(this.page);
        this.uploadDocumentPage = new UploadDocumentPage(this.page);
        this.rolesAndAccessTab = new RolesAndAccess(this.page);
        this.supplementaryResponsePage = new SupplementaryResponsePage(this.page);
        this.uploadDocumentFurtherEvidencePage = new UploadDocumentFurtherEvidencePage(this.page);
        this.updateLanguagePreferencePage = new UpdateLanguagePreferencePage(this.page);
        this.amendInterlocReviewStatePage = new AmendInterlocReviewStatePage(this.page);
        this.reviewPHEPage = new ReviewPHEPage(this.page);
        this.listingRequirementsTab = new ListingRequirements(this.page);
        this.updateUCBPage = new UpdateUCBPage(this.page);
        this.updateSubscriptionPage = new UpdateSubscriptionPage(this.page);
        this.subscriptionsTab = new Subscriptions(this.page);
        this.avTab = new AudioVideoEvidence(this.page);
        this.processAVPage = new ProcessAVPage(this.page);
        this.updateOtherPartyDataPage = new updateOtherPartyDataPage(this.page);
        this.otherPartyDetailsTab = new OtherPartyDetails(this.page);
        this.sendCaseToTcwPage = new SendCaseToTcwPage(this.page);
        this.writeFinalDecisionPage = new WriteFinalDecisionPages(page);
        this.sendToInterlocPrevalidPage = new SendToInterlocPrevalidPage(page);
        this.sendToJudgePage = new SendToJudgePage(this.page);
        this.notListablePage = new NotListablePage(this.page);
        this.updateNotListablePage = new UpdateNotListablePage(this.page);
        this.searchFilterPage = new SearchFilterPage(this.page);
        this.hearingsTab = new Hearings(this.page);
        this.listingRequirementPage = new ListingRequirementPage(this.page);
        this.reissueFurtherEvidencePage = new ReissueFurtherEvidencePage(this.page);
        this.postponementPage = new PostponementPages(this.page);
        this.prepareCaseForHearingPage = new PrepareCaseForHearingPage(this.page);
        this.reviewConfidentialityPage = new ReviewConfidentialityPage(this.page);
   }

    async loginUserWithCaseId(user, clearCacheFlag: boolean = false, caseId?: string) {
        await this.loginPage.goToLoginPage();
        await this.loginPage.verifySuccessfulLoginForUser(user, clearCacheFlag);
        await this.homePage.goToHomePage(caseId);
    }

    async loginUserWithoutCaseId(user, clearCacheFlag: boolean = false) {
        await this.loginPage.goToLoginPage();
        await this.loginPage.verifySuccessfulLoginForUser(user, clearCacheFlag);
        await this.homePage.goToCaseList;
    }

    async verifyHistoryTabDetails(state?: string, event?: string, comment?: string) {
        await this.homePage.navigateToTab("History");
        await this.homePage.delay(1000);
        /*if(state) await this.historyTab.verifyHistoryPageContentByKeyValue('End state', state);
        if(event) await this.historyTab.verifyHistoryPageContentByKeyValue('Event', event);
        if(comment) await this.historyTab.verifyHistoryPageContentByKeyValue('Comment', comment);*/
        if (event) await this.historyTab.verifyEventCompleted(event);
    }

    async verifyHistoryTabLink(linkLabel: string) {
        await this.historyTab.verifyHistoryPageEventLink(linkLabel);
    }
    async verifyOtherPartyDetails(state?: string, event?: string, comment?: string) {
        await this.homePage.navigateToTab("Other Party Details");
        await this.homePage.delay(1000);
    }
}

package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.NAME;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.SorPlaceholderService;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;

@ExtendWith(MockitoExtension.class)
class CcdNotificationServiceTest {

    SorPlaceholderService sorPlaceholderService;
    @Mock
    PlaceholderService placeholderService;
    @Mock
    CcdNotificationsPdfService ccdNotificationsPdfService;

    @Test
    void verifyCcdNotificationsPdfServiceCall() {
        CcdNotificationService ccdNotificationService = new CcdNotificationService(ccdNotificationsPdfService);
        byte[] letter = new byte[1];
        var event = EventType.ISSUE_GENERIC_LETTER;
        var recipient = "Test";
        Long caseId = 0L;
        String senderType = "Bulk Print";

        ccdNotificationService.storeNotificationLetterIntoCcd(event, letter, caseId, recipient);
        verify(ccdNotificationsPdfService, times(1))
            .mergeLetterCorrespondenceIntoCcd(eq(letter), eq(caseId), any(), eq(senderType));
    }

    @Test
    void returnToFieldOnCorrespondenceForSorLetter() {
        CcdNotificationService ccdNotificationService = new CcdNotificationService(ccdNotificationsPdfService);
        EventType eventType = EventType.SOR_WRITE;
        String name = "Test name";

        Correspondence correspondence = ccdNotificationService.buildCorrespondence(eventType, name);

        assertEquals(name, correspondence.getValue().getTo());
        assertEquals(eventType.getCcdType(), correspondence.getValue().getEventType());
    }

    @Test
    void returnRepresentativeToFieldGivenOrganisationButNoNameForSorLetter() {
        sorPlaceholderService = new SorPlaceholderService(placeholderService);
        SscsCaseData caseData = buildCaseData();
        caseData.getAppeal().getRep().setName(new Name(null, null, null));
        caseData.getAppeal().getRep().setOrganisation("Test organisation");

        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.REPRESENTATIVE_LETTER,
            Representative.class.getSimpleName(), null);

        String name = placeholders.get(NAME).toString();

        CcdNotificationService ccdNotificationService = new CcdNotificationService(ccdNotificationsPdfService);
        EventType eventType = EventType.SOR_WRITE;

        Correspondence correspondence = ccdNotificationService.buildCorrespondence(eventType, name);

        Assertions.assertEquals(correspondence.getValue().getTo(), placeholders.get(ADDRESS_NAME));
    }
}

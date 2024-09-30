package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Adjustment {
    REASONABLE_ADJUSTMENT(4, "RA0001", "Reasonable adjustment"),
    DOCUMENTS_ALTERNATIVE_FORMAT(5, "RA0002", "I need documents in an alternative format"),
    HELP_WITH_FORMS(6, "RA0003", "I need help with forms"),
    GET_TO_INTO_AROUND_BUILDINGS(7, "RA0004", "I need adjustments to get to, into and around our buildings"),
    BRING_SUPPORT_HEARING(8, "RA0005", "I need to bring support with me to a hearing"),
    COMFORTABLE_DURING_HEARING(9, "RA0006", "I need something to feel comfortable during my hearing"),
    CERTAIN_TYPE_HEARING(10, "RA0007", "I need to request a certain type of hearing"),
    COMMUNICATING_UNDERSTANDING(11, "RA0008", "I need help communicating and understanding"),
    HEARING_ENHANCEMENT_SYSTEM(12, "RA0009", "I need an Hearing Enhancement System (Hearing/Induction Loop, Infrared Receiver)"),
    DOCUMENTS_SPECIFIED_COLOUR(13, "RA0010", "Documents in a specified colour"),
    DOCUMENTS_EASY_READ_FORMAT(14, "RA0011", "Documents in easy read format"),
    BRAILLE_DOCUMENTS(15, "RA0012", "Braille documents"),
    DOCUMENTS_LARGE_PRINT(16, "RA0013", "Documents in large print"),
    AUDIO_TRANSLATION_DOCUMENTS(17, "RA0014", "Audio translation of documents"),
    DOCUMENTS_READ_OUT(18, "RA0015", "Documents read out to me"),
    INFORMATION_EMAILED(19, "RA0016", "Information emailed to me"),
    GUIDANCE_COMPLETE_FORMS(20, "RA0017", "Guidance on how to complete forms"),
    SUPPORT_FILLING_FORMS(21, "RA0018", "Support filling in forms"),
    STEP_FREE_WHEELCHAIR_ACCESS(22, "RA0019", "Step free / wheelchair access"),
    VENUE_WHEELCHAIR(23, "RA0020", "Use of venue wheelchair"),
    PARKING_SPACE_CLOSE_VENUE(24, "RA0021", "Parking space close to the venue"),
    ACCESSIBLE_TOILET(25, "RA0022", "Accessible toilet"),
    HELP_USING_LIFT(26, "RA0023", "Help using a lift"),
    DIFFERENT_TYPE_CHAIR(27, "RA0024", "A different type of chair"),
    GUIDING_IN_THE_BUILDING(28, "RA0025", "Guiding in the building"),
    SUPPORT_WORKER_OR_CARER_WITH(29, "RA0026", "Support worker or carer with me"),
    FRIEND_OR_FAMILY_WITH(30, "RA0027", "Friend or family with me"),
    ASSISTANCE_GUIDE_DOG(31, "RA0028", "Assistance / guide dog"),
    THERAPY_ANIMAL(32, "RA0029", "Therapy animal"),
    APPROPRIATE_LIGHTING(33, "RA0030", "Appropriate lighting"),
    REGULAR_BREAKS(34, "RA0031", "Regular breaks"),
    ABLE_TO_MOVE_AROUND(35, "RA0032", "Space to be able to get up and move around"),
    PRIVATE_WAITING_AREA(36, "RA0033", "Private waiting area"),
    IN_PERSON_HEARING(37, "RA0034", "In person hearing"),
    VIDEO_HEARING(38, "RA0035", "Video hearing"),
    PHONE_HEARING(39, "RA0036", "Phone hearing"),
    EXTRA_TIME_THINK_EXPLAIN(40, "RA0037", "Extra time to think and explain myself"),
    INTERMEDIARY(41, "RA0038", "Intermediary"),
    SPEECH_TO_TEXT(42, "RA0039", "Speech to text reporter (palantypist)"),
    CLOSE_TO_SPEAKING(43, "RA0040", "Need to be close to who is speaking"),
    LIP_SPEAKER(44, "RA0041", "Lip speaker"),
    SIGN_LANGUAGE_INTERPRETER(45, "RA0042", "Sign Language Interpreter"),
    HEARING_LOOP(46, "RA0043", "Hearing loop (hearing enhancement system)"),
    INFRARED_RECEIVER(47, "RA0044", "Infrared receiver (hearing enhancement system)"),
    INDUCTION_LOOP(48, "RA0045", "Induction loop (hearing enhancement system)"),
    VISIT_TO_BEFORE_THE_HEARING(49, "RA0046", "Visit to court or tribunal before the hearing"),
    EXPLANATION_COURT_WHO_AT_HEARING(50, "RA0047", "Explanation of the court and who's in the room at the hearing");

    private final int id;
    @JsonValue
    private final String flagCode;
    private final String descriptionEn;
}

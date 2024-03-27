package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import java.util.UUID;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class PlaceholderHelper {

    private PlaceholderHelper() {

    }

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";


    public static SscsCaseData buildCaseData() {

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        return SscsCaseData.builder()
            .ccdCaseId("123456")
            .caseReference("SC123/12/1234")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .address(Address.builder()
                        .line1("HM Courts & Tribunals Service")
                        .line2("Down the road")
                        .town("Social Security & Child Support Appeals")
                        .county("Prudential Buildings")
                        .postcode("L2 5UZ")
                        .build())
                    .build())
                .build())
            .build();
    }

    public static SscsCaseData buildCaseDataWithoutBenefitType() {

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
                .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        return SscsCaseData.builder()
                .ccdCaseId("123456")
                .caseReference("SC123/12/1234")
                .regionalProcessingCenter(rpc)
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                                .identity(Identity.builder().nino("JT0123456B").build())
                                .address(Address.builder()
                                        .line1("HM Courts & Tribunals Service")
                                        .line2("Down the road")
                                        .town("Social Security & Child Support Appeals")
                                        .county("Prudential Buildings")
                                        .postcode("L2 5UZ")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public static JointParty buildJointParty() {
        Name name = Name.builder()
            .title("Ms")
            .firstName("Joint")
            .lastName("Party")
            .build();

        Address address = Address.builder()
            .town("London")
            .postcode("T4 4JP")
            .line1("JP address line 1")
            .line2("JP address line 2")
            .build();


        return JointParty.builder()
            .name(name)
            .id(UUID.randomUUID().toString())
            .address(address)
            .hasJointParty(YesNo.YES)
            .build();
    }

    public static OtherParty buildOtherParty() {
        Name otherPartyName = Name.builder()
            .title("Mr")
            .firstName("Other")
            .lastName("Party")
            .build();

        Address address = Address.builder()
            .town("London")
            .postcode("T4 4OP")
            .line1("OP address line 1")
            .line2("OP address line 2")
            .build();

        return OtherParty.builder()
            .id(UUID.randomUUID().toString())
            .name(otherPartyName)
            .address(address)
            .build();
    }
}

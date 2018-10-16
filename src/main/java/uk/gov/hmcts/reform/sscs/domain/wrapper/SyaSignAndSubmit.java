package uk.gov.hmcts.reform.sscs.domain.wrapper;

public class SyaSignAndSubmit {

    private String signer;

    public SyaSignAndSubmit() {
        //
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    @Override
    public String toString() {
        return "SyaSignAndSubmit{"
                + "signer='" + signer + '\''
                + '}';
    }
}

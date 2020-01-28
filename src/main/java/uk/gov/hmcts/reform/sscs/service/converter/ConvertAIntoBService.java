package uk.gov.hmcts.reform.sscs.service.converter;

public interface ConvertAIntoBService<A, B> {
    B convert(A source);
}

package uk.gov.hmcts.reform.sscs.service.converter;

public interface ConvertAintoBService<A, B> {
    B convert(A source);
}

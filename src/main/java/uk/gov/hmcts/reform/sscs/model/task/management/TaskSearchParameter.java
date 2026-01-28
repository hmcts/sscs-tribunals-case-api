package uk.gov.hmcts.reform.sscs.model.task.management;

public interface TaskSearchParameter<T> {

    TaskSearchParameterKey getKey();

    TaskSearchOperator getOperator();

    T getValues();
}

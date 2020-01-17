//package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import java.io.Serializable;
//import java.util.Objects;
//import javax.validation.constraints.NotNull;
//
///**
// * A DTO for the DocumentTask entity.
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class DocumentTaskDTO implements Serializable {
//
//    private Long id;
//
//    @NotNull
//    private StitchingBundleDTO  bundle;
//
//    private TaskState taskState = TaskState.NEW;
//
//    private String failureDescription;
//
//    private CallbackDto callback;
//
//    @JsonIgnore
//    private String jwt;
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public StitchingBundleDTO getBundle() {
//        return bundle;
//    }
//
//    public void setBundle(StitchingBundleDTO bundle) {
//        this.bundle = bundle;
//    }
//
//    public TaskState getTaskState() {
//        return taskState;
//    }
//
//    public void setTaskState(TaskState taskState) {
//        this.taskState = taskState;
//    }
//
//    public String getFailureDescription() {
//        return failureDescription;
//    }
//
//    public void setFailureDescription(String failureDescription) {
//        this.failureDescription = failureDescription;
//    }
//
//    public String getJwt() {
//        return jwt;
//    }
//
//    public void setJwt(String jwt) {
//        this.jwt = jwt;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) {
//            return true;
//        }
//        if (o == null || getClass() != o.getClass()) {
//            return false;
//        }
//
//        DocumentTaskDTO documentTaskDTO = (DocumentTaskDTO) o;
//        if (documentTaskDTO.getId() == null || getId() == null) {
//            return false;
//        }
//        return Objects.equals(getId(), documentTaskDTO.getId());
//    }
//
//    public CallbackDto getCallback() {
//        return callback;
//    }
//
//    public void setCallback(CallbackDto callback) {
//        this.callback = callback;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hashCode(getId());
//    }
//
//
//    public String toString() {
//        return "DocumentTaskDTO(id=" + this.getId() + ", bundle=" + this.getBundle()
//                + ", taskState=" + this.getTaskState() + ", failureDescription=" + this.getFailureDescription()
//                + ", callback=" + this.getCallback() + ", jwt=" + this.getJwt() + ")";
//    }
//}

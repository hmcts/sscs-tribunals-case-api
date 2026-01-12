package uk.gov.hmcts.reform.sscs;

public final class RequestSnapshotHolder {

    private static final ThreadLocal<RequestSnapshot> TL = new ThreadLocal<>();

    private RequestSnapshotHolder() {

    }

    public static void set(RequestSnapshot snapshot) {
        TL.set(snapshot);
    }

    public static RequestSnapshot get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }

    public record RequestSnapshot(String authorization) {
    }
}


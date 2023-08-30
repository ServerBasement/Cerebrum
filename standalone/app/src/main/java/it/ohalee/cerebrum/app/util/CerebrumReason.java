package it.ohalee.cerebrum.app.util;

public enum CerebrumReason {

    OK(1),
    ERROR(2),
    RANCH_ERROR(3),
    SERVER_ERROR(4);

    private final int code;

    CerebrumReason(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

}

package it.ohalee.cerebrum.app.util;

public class CerebrumError {

    private final CerebrumReason code;
    private final String reason;

    public CerebrumError(CerebrumReason code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public static String evaluate(CerebrumError handle, String successValue) {
        if (handle.code() == CerebrumReason.OK)
            return successValue;
        return handle.reason();
    }

    public static CerebrumError of(CerebrumReason code, String reason) {
        return new CerebrumError(code, reason);
    }

    public String reason() {
        return reason;
    }

    public CerebrumReason code() {
        return code;
    }
}

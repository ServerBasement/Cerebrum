package it.ohalee.cerebrum.app.util;

import it.ohalee.cerebrum.app.Logger;

public class Validate {

    public static <T> T notNull(final T object) {
        return notNull(object, "The validated object is null");
    }

    public static <T> T notNull(final T obj, final String message, final Object... values) {
        if (obj == null) {
            Logger.severe(String.format(message, values));
            throw new NullPointerException(String.format(message, values));
        }
        return obj;
    }

}

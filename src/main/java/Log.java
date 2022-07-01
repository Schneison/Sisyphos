public class Log {
    public static final boolean LOGGING = Environment.LOGGING;
    public static final boolean DEBUG_LOGGING = Environment.DEBUG;

    private Log() {
    }

    public static void debug(Object x) {
        if (DEBUG_LOGGING) {
            info(x);
        }
    }

    public static void info(Object x) {
        if (LOGGING) {
            log(x);
        }
    }

    public static void log(Object x) {
        System.out.println(x);
    }
}

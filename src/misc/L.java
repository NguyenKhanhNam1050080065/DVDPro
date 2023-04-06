package misc;

import com.cycastic.javabase.dispatcher.AsyncEngine;

public class L {
    private static  final int type_sep_len = 25;
    private static final boolean verbose = true;
    private static final boolean error = true;
    private static final L singleton = new L();
    private final AsyncEngine engine;

    public L(){
        engine = new AsyncEngine(AsyncEngine.MODE_HOT);
    }

    private static String build_type(String type){
        return " + " + type +
                " ".repeat(Math.max(0, type_sep_len - type.length() - 3)) +
                " | ";
    }

    private void _log(String type, String format){
        System.out.printf("%s%s%n", build_type(type), format);
    }
    private void _err(String type, String format){
        System.err.printf("%s%s%n", build_type(type), format);
    }
    private void _decorate(String message, int padding) {
        // Chuyển 1 tab thành 4 space
        // Không hỗ trợ newline lololol
        String writable = message.replace("\t", "    ").replace('\n', ' ');
        StringBuilder pads = new StringBuilder();
        pads.append(" ".repeat(Math.max(0, padding)));
        writable = pads + writable + pads;
        StringBuilder separators = new StringBuilder();
        separators.append("/".repeat(writable.length()));
        System.err.printf("%n///%s///%n///%s///%n///%s///%n%n", separators, writable, separators);
    }
    @Deprecated(since = "0.7")
    public static void init() {
//        new L();
    }
    public static void destroy() {
        singleton.engine.terminate();
    }
    //
    // Màu mè hóa việc logging :v
    //
    public static void log(String type, String message) {
        if (!verbose) return;
        singleton.engine.dispatch(() -> singleton._log(type, message));
    }
    public static void err(String type, String message) {
        if (!error) return;
        singleton.engine.dispatch(() -> singleton._err(type, message));
    }
    public static void decorate(String message, int padding) {
        singleton.engine.dispatch(() -> singleton._decorate(message, padding));
    }
    public static void decorate(String message) {
        decorate(message, 4);
    }
}

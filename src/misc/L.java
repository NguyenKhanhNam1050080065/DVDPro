package misc;

import com.cycastic.javabase.dispatcher.Command;
import com.cycastic.javabase.dispatcher.ExecutionLoop;

public class L implements Command {
    private static  final int type_sep_len = 25;
    private static final boolean verbose = true;
    private static final boolean error = true;
    private static final Object lock = new Object();

    private static L singleton = new L();
    private ExecutionLoop main_loop;

    public L(){
        if (singleton != null) return;
        singleton = this;
        main_loop = new ExecutionLoop();
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
        writable = pads.toString() + writable + pads.toString();
        StringBuilder separators = new StringBuilder();
        separators.append("/".repeat(writable.length()));
        synchronized (lock){
            System.err.printf("%n///%s///%n///%s///%n///%s///%n%n", separators, writable, separators);
        }
    }
    @Override
    public void exec(Object... params) {
        String method = params[0].toString();
        switch (method){
            case "_log" -> {
                _log(params[1].toString(), params[2].toString());
            }
            case "_err" -> {
                _err(params[1].toString(), params[2].toString());
            }
            case "_decorate" -> {
                _decorate(params[1].toString(), (Integer)params[2]);
            }
            default -> {
                _log("L", "Unsupported command");
            }
        }
    }
    @Deprecated(since = "0.7")
    public static void init() {
//        new L();
    }
    public static void destroy() {
//        log("L", "Logging server is closing...");
        singleton.main_loop.terminate();
    }
    //
    // Màu mè hóa việc logging :v
    //
    public static void log(String type, String message) {
        if (!verbose) return;
        singleton.main_loop.push(singleton, "_log", type, message);
    }
    public static void err(String type, String message) {
        if (!error) return;
        singleton.main_loop.push(singleton, "_err", type, message);
    }
    public static void decorate(String message, int padding) {
        singleton.main_loop.push(singleton, "_decorate", message, padding);
    }
    public static void decorate(String message) {
        singleton.main_loop.push(singleton, "_decorate", message, 4);
    }
}

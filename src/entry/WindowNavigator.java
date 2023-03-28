package entry;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.Scanner;

import com.cycastic.javabase.auth.FirebaseAuth;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.cycastic.javabase.firestore.Firestore;
import com.cycastic.javabase.misc.FirebaseConfig;
import ui.auth_window.AuthWindow;
import ui.main_window.MainWindow;
import misc.L;

import javax.swing.*;

public class WindowNavigator {
    public static final int AUTO_SHUTDOWN = 10;
    private static final WindowNavigator singleton = new WindowNavigator();
    private Window currentMainWindow;
    private final FirebaseAuth authModule;
    private final Firestore dbModule;
    private final CacheServer cacheServer;
    private final CapsulesJob capsulesJob;

    private static String readFirebaseConfig(){
        try {
            L.log("WindowNavigator", "Trying to read config file in the same directory");
            File f = new File("firebase_config.json");
            Scanner scan = new Scanner(f);
            StringBuilder s = new StringBuilder();
            while (scan.hasNextLine()){
                s.append(scan.nextLine());
            }
            scan.close();
            return s.toString();
        } catch (FileNotFoundException e){
            L.log("WindowNavigator", "Trying to read config file in the archive");
            try {
                InputStream input = WindowNavigator.class.getResourceAsStream("/firebase_config.json");
                byte[] charArray = input.readAllBytes();
                StringBuilder re = new StringBuilder();
                for (byte b : charArray) {
                    re.append((char)b);
                }
                return re.toString();
            } catch (IOException ex){
                return "";
            }
        }
    }
    private void restore(){
        cacheServer.restore();
    }
    private void flush(){
//        capsulesJob.flush();
//
//        cacheServer.flush();
    }
    public static String getJarLoc() {
        try {
            return new File(WindowNavigator.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getPath();
        } catch (URISyntaxException e){
            return "";
        }
    }
    public static String combinePath(String p1, String p2){
        File f1 = new File(p1);
        File f2 = new File(f1, p2);
        return f2.getPath();
    }
    public WindowNavigator(){
        authModule = new FirebaseAuth();
        dbModule = new Firestore(true);
        cacheServer = new CacheServer(combinePath(getJarLoc(), "cache.bin"));
        restore();

        capsulesJob = new CapsulesJob(cacheServer);
    }
    public static void setup(){
        FlatDarculaLaf.setup();
        String cfg = readFirebaseConfig();
        if (cfg.isEmpty()){
            Window host = singleton.currentMainWindow;
            JOptionPane.showMessageDialog(host, "Không đọc được cài đặt Firebase");
            System.exit(1);
        }
        FirebaseConfig config = new FirebaseConfig(cfg);
        auth().enrollConfig(config);
        db().enrollConfig(config);
    }
    public static void shutdown(){
        Thread shutdownDaemon = new Thread(() -> {
            long epoch = new GregorianCalendar().getTime().getTime();
            while (true){
                try {
                    Thread.sleep(100);
                    long now = new GregorianCalendar().getTime().getTime();
                    if ((now - epoch) / 1000L >= AUTO_SHUTDOWN)
                        System.exit(0);
                } catch (InterruptedException ignored) {}
            }
        });
        shutdownDaemon.setDaemon(true);
        shutdownDaemon.start();
        L.log("WindowNavigator", "Shutdown timer has started");
        singleton.flush();
        auth().terminate();
        db().terminate();
        L.destroy();
    }
    public static FirebaseAuth auth() { return singleton.authModule; }
    public static Firestore db() { return singleton.dbModule; }
    public static CacheServer cache() { return singleton.cacheServer; }
    public static CapsulesJob capsules() { return singleton.capsulesJob; }
    public static AuthWindow createAuthWindow(){
        synchronized (singleton){
            if (singleton.currentMainWindow != null) return null;
            AuthWindow aw = new AuthWindow();
            singleton.currentMainWindow = aw;
            return aw;
        }
    }
    public static MainWindow createMainWindow(){
        synchronized (singleton){
            if (singleton.currentMainWindow != null) singleton.currentMainWindow.dispose();
            MainWindow mw = new MainWindow();
            singleton.currentMainWindow = mw;
            return mw;
        }
    }
    public static void closeWindow(Window win){
        synchronized (singleton){
            if (win == singleton.currentMainWindow) {
                singleton.currentMainWindow = null;
                shutdown();
            }
            win.dispose();
        }
    }
    public static GraphicsDevice getGraphicsDevice(Window win){
        GraphicsDevice gd = null;
        try{
            gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        } catch (HeadlessException e) {
            L.log("AuthWindow", "This application is running in headless mode and cannot continue.");
            WindowNavigator.closeWindow(win);
        }
        return gd;
    }
    public static Point getCenterOfScreen(Window win, Dimension dm){
        GraphicsDevice gd = getGraphicsDevice(win);
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        // Căn cửa sổ giữa màn hình
        if (width > dm.width && height > dm.height){
            return new Point((width / 2) - (dm.width / 2), (height / 2) - (dm.height / 2));
        }
        return new Point(0, 0);
    }
}

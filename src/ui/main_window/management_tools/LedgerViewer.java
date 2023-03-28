package ui.main_window.management_tools;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;
import misc.L;
import models.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class LedgerViewer extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(400, 400);
    private JPanel MainPanel;
    private JTextField f_id;
    private JTextField f_type;
    private JTextField f_time;
    private JTextField f_commissioner;
    private JTextField f_vendor;
    private JTextField f_product;
    private JTextField f_amount;
    private final SafeFlag returnLock = new SafeFlag(false);
    public void changeInputState(boolean state){
        f_id.setEnabled(state);
        f_type.setEnabled(state);
        f_time.setEnabled(state);
        f_commissioner.setEnabled(state);
        f_vendor.setEnabled(state);
        f_product.setEnabled(state);
        f_amount.setEnabled(state);
    }

    public LedgerViewer(LedgerModel model){
        super("Thông tin giao dịch");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                returnLock.set();
                dispose();
            }
        });
        changeInputState(false);
        new Thread(() -> {
            f_id.setText(model.getDocumentName());
            f_type.setText(model.getInput() ? "Nhập kho" : "Xuất kho");
            f_time.setText(LocalDateTime.ofInstant(Instant.ofEpochSecond(model.getTime()), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            f_amount.setText(String.valueOf(model.getAmount()));

            FirestoreQuery getDvd = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("dvds_info").where("imdb_link", FirestoreQuery.Operator.EQUAL, model.getDvd());
            FirestoreQuery getVendor = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("vendors").where("guid", FirestoreQuery.Operator.EQUAL, model.getVendor());
            FirestoreQuery getUser = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("users").where("uid", FirestoreQuery.Operator.EQUAL, model.getCommissioner());
            DVDCollection dvdCollection = new DVDCollection(WindowNavigator.db(), true, getDvd);
            VendorCollection vendorCollection = new VendorCollection(WindowNavigator.db(), true, getVendor);
            try {
                Map<String, DVDModel> dvd = dvdCollection.getDocuments();
                for (Map.Entry<String, DVDModel> E : dvd.entrySet()){
                    f_product.setText(E.getValue().getName());
                    f_product.setToolTipText("ID: %s".formatted(E.getValue().getImdbLink()));
                    break;
                }
            } catch (FirestoreModelException ex){
                f_product.setText("<Failed>");
                L.log("LedgerViewer", ex.toString());
            }
            try {
                Map<String, VendorModel> dvd = vendorCollection.getDocuments();
                for (Map.Entry<String, VendorModel> E : dvd.entrySet()){
                    f_vendor.setText(E.getValue().getName());
                    f_vendor.setToolTipText("ID: %s".formatted(E.getValue().getGuid()));
                    break;
                }
            } catch (FirestoreModelException ex){
                f_vendor.setText("<Failed>");
                L.log("LedgerViewer", ex.toString());
            }
            final SafeFlag finished = new SafeFlag(false);
            WindowNavigator.db().query(getUser, new FirestoreTaskReceiver() {
                @Override
                public void connectionFailed() {
                    f_commissioner.setText("<Failed>");
                    L.log("LedgerViewer", "Failed to get user: No connection");
                    finished.set();
                }
                @Override
                public void requestFailed(int i, Map<String, String> map, String s) {
                    f_commissioner.setText("<Failed>");
                    L.log("LedgerViewer", "Failed to get user: Code %d: %s".formatted(i, s));
                    finished.set();
                }
                @Override
                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                    try {
                        for (Map.Entry<String, FirestoreDocument> E : firestoreTaskResult.getDocuments().entrySet()){
                            f_commissioner.setText(E.getValue().getFields().get("username").toString());
                            f_commissioner.setToolTipText("ID: %s".formatted(E.getKey()));
                            break;
                        }
                    } catch (RuntimeException ex){
                        f_commissioner.setText("Failed");
                        L.log("LedgerViewer", "Failed to set commissioner: %s".formatted(ex.toString()));
                    }
                    finished.set();
                }
            });
            finished.waitToFinish();
            changeInputState(true);
        }).start();
    }
    public void run(){
        returnLock.waitToFinish();
    }
}

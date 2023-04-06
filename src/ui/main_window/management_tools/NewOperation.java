package ui.main_window.management_tools;

import com.cycastic.javabase.auth.FirebaseAuthToken;
import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;
import misc.L;
import models.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Map;

public class NewOperation extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(450, 300);
    public static final int RETRY_TIME = 5;
    public static final int INPUT_MAX_AMOUNT = 1000;
    private JLabel l_operation;
    private JButton b_return;
    private JButton b_accept;
    private JComboBox<String> f_vendors;
    private JTextField tf_amount;
    private JTextField tf_total;
    private JPanel MainPanel;
    private JTextField f_operation;

    private final SafeFlag returnLock = new SafeFlag(false);
//    private final SafeFlag editing = new SafeFlag(false);
    private final java.util.List<String> vendorsId = new ArrayList<>();
    private String cachedDvdId = "";
    private long cachedAmount = 0;

    public void changeInputState(boolean state){
        f_vendors.setEnabled(state);
        tf_amount.setEnabled(state);
        b_accept.setEnabled(state);
        b_return.setEnabled(state);
    }
    public void lockInput(){
        changeInputState(false);
    }
    public void unlockInput(){
        changeInputState(true);
    }
    public void fetchSettings(boolean isInput, String link){
        synchronized (this){
            lockInput();
            vendorsId.clear();
            f_vendors.removeAllItems();
            final NewOperation no = this;
            FirestoreQuery getVendors = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("vendors").where("guid", FirestoreQuery.Operator.NOT_EQUAL, "");
            FirestoreQuery getCurrAmount = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("dvds_info").where("imdb_link", FirestoreQuery.Operator.EQUAL, link);
            try {
                VendorCollection vendorCollection = new VendorCollection(WindowNavigator.db(), true, getVendors);
                Map<String, VendorModel> vendors = vendorCollection.getDocuments();
                int count = vendors.size();
                for (Map.Entry<String, VendorModel> E : vendors.entrySet()){
                    VendorModel model = E.getValue();
                    vendorsId.add(model.getGuid());
                    f_vendors.addItem(model.getName());
                }
                if (count > 0)
                    f_vendors.setSelectedIndex(0);
            } catch (FirestoreModelException ex){
                if (ex.getClass() == NoConnectionException.class){
                    JOptionPane.showMessageDialog(no, "Kết nối thất bại: Không có Internet");
                } else if (ex.getClass() == QueryRefusedException.class) {
                    JOptionPane.showMessageDialog(no, "Kết nối thất bại: Yêu cầu bị từ chối");
                }
                L.log("NewOperation", ex.toString());
            }
            if (isInput){
                tf_total.setText(String.valueOf(INPUT_MAX_AMOUNT));
            } else {
                tf_total.setText(String.valueOf(cachedAmount));
            }
            unlockInput();
        }
    }
    private boolean commit(boolean isInput, String vendorId, String imdbLink, long oldAmount, long amountDelta){
        long unixTimeNow = new GregorianCalendar().getTime().getTime() / 1000;
        final long direction = isInput ? 1L : -1L;
        final long newAmount = Math.max(0, oldAmount + direction * amountDelta);

        FirestoreQuery newOp = new FirestoreQuery(FirestoreQuery.QueryType.CREATE_DOCUMENT)
                .onCollection("ledger");
        FirebaseAuthToken authToken = WindowNavigator.auth().getAuthWrapper().getAuthToken();
        LedgerModel ledger = new LedgerModel(WindowNavigator.db(), true);
        ledger.setAmount(amountDelta);
        ledger.setDvd(imdbLink);
        ledger.setInput(isInput);
        ledger.setTime(unixTimeNow);
        ledger.setVendor(vendorId);
        if (authToken != null)
            ledger.setCommissioner(authToken.getLocalId());
        LedgerCollection ledgerCollection = new LedgerCollection(WindowNavigator.db(), true, newOp);
        try {
            ledgerCollection.serializeSingle(ledger);
            ledgerCollection.evaluate();
        } catch (FirestoreModelException e){
            L.log("NewOperation", e.toString());
            return false;
        }

        FirestoreQuery patchOp = new FirestoreQuery(FirestoreQuery.QueryType.PATCH_DOCUMENT)
                .onCollection("dvds_info").onDocument(cachedDvdId).documentExisted(true).updateMask("stock");
        DVDModel dvd = new DVDModel(WindowNavigator.db(), true);
        dvd.setStock(newAmount);
        DVDCollection dvdCollection = new DVDCollection(WindowNavigator.db(), true, patchOp);
        for (long i = 0; /*i < RETRY_TIME*/ true; i++){
            try {
                dvdCollection.serializeSingle(dvd);
                dvdCollection.evaluate();
                break;
            } catch (FirestoreModelException e){
                L.log("NewOperation", "Attempt No.%d failed: %s".formatted(i + 1, e.toString()));
            }
        }
        return true;
    }
    private void afterEvent(Runnable event){
        if (!returnLock.get()) returnLock.set();
//        new Thread(this::dispose).start();
        dispose();
        if (event != null) event.run();
    }
    public NewOperation(String imdbLink, String dvdTitle, String dvdId, long stock, boolean isInput){
        this(imdbLink, dvdTitle, dvdId, stock, isInput, null);
    }
    public NewOperation(String imdbLink, String dvdTitle, String dvdId, long stock, boolean isInput, Runnable finalize){
        super("Nhập/Xuất kho");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);

        cachedDvdId = dvdId;
        cachedAmount = stock;
        l_operation.setText((isInput ? "Nhập kho" : "Xuất kho") + ": " + dvdTitle);
        f_operation.setText((isInput ? "Nhập kho" : "Xuất kho"));
        fetchSettings(isInput, imdbLink);

        final NewOperation no = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                afterEvent(finalize);
            }
        });
        b_return.addActionListener(e -> {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });
        b_accept.addActionListener(e -> {
            new Thread(()-> {
                synchronized (this){
                    final long amountLong;
                    final long maxAmount = Long.parseLong(tf_total.getText());
                    try {
                        amountLong = Long.parseLong(tf_amount.getText());
                    } catch (NumberFormatException ex){
                        JOptionPane.showMessageDialog(no, "Giá trị đầu vào không hợp lệ: %s".formatted(tf_amount.getText()));
                        return;
                    }
                    if (amountLong > maxAmount && !isInput){
                        JOptionPane.showMessageDialog(no, "Số lượng xuất lớn hơn số lượng tồn kho");
                        return;
                    }
                    if (vendorsId.size() == 0 || maxAmount <= 0 || cachedDvdId.isEmpty()){
                        JOptionPane.showMessageDialog(no, "Không thể kiểm tra thông tin kho");
                        return;
                    }
                    lockInput();
                    int idx = f_vendors.getSelectedIndex();
                    if (idx == -1){
                        unlockInput();
                        return;
                    }
                    String vendorId = vendorsId.get(idx);
                    if (commit(isInput, vendorId, imdbLink, cachedAmount, amountLong))
                        JOptionPane.showMessageDialog(no, "Thành công");
                    else
                        JOptionPane.showMessageDialog(no, "Thất bại");
                    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                }
            }).start();
        });
    }
    public void run(){
        returnLock.waitToFinish();
    }
}

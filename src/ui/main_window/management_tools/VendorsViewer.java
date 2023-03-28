package ui.main_window.management_tools;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;
import misc.L;
import models.VendorCollection;
import models.VendorModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class VendorsViewer extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(600, 400);
    private JList<String> vendorsList;
    private JButton b_return;
    private JButton b_remove;
    private JButton b_insert;
    private JPanel MainPanel;
    private JButton b_update;

    private final DefaultListModel<String> listModel;
    private final java.util.List<VendorModel> vendors = new java.util.ArrayList();
    private final SafeFlag returnLock = new SafeFlag(false);
    private final SafeFlag editing = new SafeFlag(false);

    private void finisher(){
        returnLock.set();
        dispose();
    }
    public void changeInputState(boolean state){
        b_insert.setEnabled(state);
        b_remove.setEnabled(false);
        b_update.setEnabled(false);
        b_return.setEnabled(state);
        vendorsList.setEnabled(state);
    }
    public void lockInput(){
        changeInputState(false);
    }
    public void unlockInput(){
        changeInputState(true);
    }
    private void refreshVendorList(){
        new Thread(() -> {
            synchronized (returnLock){
                editing.set();
                lockInput();

                vendorsList.clearSelection();
                listModel.clear();
                vendors.clear();
                FirestoreQuery getVendors = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                        .from("vendors").where("guid", FirestoreQuery.Operator.NOT_EQUAL, "");
                try {
                    VendorCollection vendorCollection = new VendorCollection(WindowNavigator.db(), true, getVendors);
                    Map<String, VendorModel> vendorsList = vendorCollection.getDocuments();
                    for (Map.Entry<String, VendorModel> E : vendorsList.entrySet()){
                        VendorModel model = E.getValue();
                        listModel.addElement(model.getName());
                        vendors.add(model);
                    }
                } catch (FirestoreModelException ex){
                    if (ex.getClass() == NoConnectionException.class){
                        JOptionPane.showMessageDialog(this, "Kết nối thất bại: Không có Internet");
                    } else if (ex.getClass() == QueryRefusedException.class) {
                        JOptionPane.showMessageDialog(this, "Kết nối thất bại: Yêu cầu bị từ chối");
                    }
                    L.log("VendorsViewer", ex.toString());
                }
                unlockInput();
                editing.clear();
            }
        }).start();
    }
    private void viewVendor(){
        new Thread(() -> {
            synchronized (returnLock){
                int idx = vendorsList.getSelectedIndex();
                if (idx == -1) return;
                lockInput();
                new VendorUpdate(VendorUpdate.MODE_VIEW, vendors.get(idx)).run();
                unlockInput();
            }
        }).start();
    }

    public VendorsViewer(){
        super("Đối tác");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        final VendorsViewer vv = this;

        listModel = (DefaultListModel<String>) vendorsList.getModel();
        vv.refreshVendorList();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finisher();
            }
        });
        b_return.addActionListener(e -> {
            synchronized (returnLock){
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }
        });
        b_remove.addActionListener(e -> new Thread(() -> {
            synchronized (returnLock){
                int[] indices = vendorsList.getSelectedIndices();
                final int total = indices.length;
                if (total == 0) return;
                int choice = JOptionPane.showConfirmDialog(this, "Xóa %d đối tác?".formatted(total));
                if (choice != JOptionPane.YES_OPTION) return;
                lockInput();
                final AtomicInteger removed = new AtomicInteger(0);
                for (final int index : indices) {
                    final SafeFlag flag = new SafeFlag(false);
                    FirestoreQuery removeVendor = new FirestoreQuery(FirestoreQuery.QueryType.DELETE_DOCUMENT)
                            .onCollection("vendors").onDocument(vendors.get(index).getDocumentName());
                    WindowNavigator.db().query(removeVendor, new FirestoreTaskReceiver() {
                        @Override
                        public void connectionFailed() {
                            flag.set();
                        }
                        @Override
                        public void requestFailed(int i, Map<String, String> map, String s) {
                            L.log("VendorsViewer", "Failed to remove vendor index %d: %s".formatted(index, s));
                            flag.set();
                        }
                        @Override
                        public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                            removed.incrementAndGet();
                            flag.set();
                        }
                    });
                    flag.waitToFinish();
                }
                JOptionPane.showMessageDialog(this, "Đã xóa %d/%d đối tác".formatted(removed.get(), total));
                refreshVendorList();
            }
        }).start());
        b_insert.addActionListener(e -> new Thread(() -> {
            synchronized (returnLock){
                lockInput();
                final boolean success = new VendorUpdate(VendorUpdate.MODE_CREATE, null).run();
                if (success) refreshVendorList();
                else unlockInput();
            }
        }).start());
        vendorsList.addListSelectionListener(e -> {
            if (editing.get()) return;
            b_remove.setEnabled(vendorsList.getSelectedIndices().length != 0);
            b_update.setEnabled(b_remove.isEnabled());
        });
        b_update.addActionListener(e -> new Thread(() -> {
            synchronized (returnLock){
                int idx = vendorsList.getSelectedIndex();
                if (idx == -1) return;
                lockInput();
                final boolean success = new VendorUpdate(VendorUpdate.MODE_UPDATE, vendors.get(idx)).run();
                if (success) refreshVendorList();
                else unlockInput();
            }
        }).start());
        vendorsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) viewVendor();
            }
        });
    }
    public void run(){
        returnLock.waitToFinish();
    }
    private void createUIComponents() {
        // TODO: place custom component creation code here
        vendorsList = new JList<>(new DefaultListModel<>());
    }
}

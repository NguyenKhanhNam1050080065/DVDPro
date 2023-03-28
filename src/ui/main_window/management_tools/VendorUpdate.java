package ui.main_window.management_tools;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.FirestoreModelException;
import com.cycastic.javabase.firestore.FirestoreQuery;
import com.cycastic.javabase.firestore.NoConnectionException;
import com.cycastic.javabase.firestore.QueryRefusedException;
import entry.WindowNavigator;
import misc.L;
import models.VendorCollection;
import models.VendorModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;

public class VendorUpdate extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(400, 300);
    public static final int MODE_CREATE = 0;
    public static final int MODE_UPDATE = 1;
    public static final int MODE_VIEW   = 2;
    private JPanel MainPanel;
    private JButton b_return;
    private JButton b_insert;
    private JTextField f_id;
    private JTextField f_name;
    private JTextField f_email;
    private JLabel f_title;
    private final int selectedMode;
    private final SafeFlag returnLock = new SafeFlag(false);
    private final SafeFlag changed = new SafeFlag(false);
    private VendorModel currentVendor;

    private void finisher(){
        returnLock.set();
        dispose();
    }
    private void applyMode(){
        final boolean isViewing = selectedMode == MODE_VIEW;
        f_name.setEditable(!isViewing);
        f_email.setEditable(!isViewing);
        b_insert.setEnabled(!isViewing);
        b_insert.setVisible(!isViewing);
    }
    private void displayInfo(){
        if (selectedMode != MODE_CREATE){
            f_id.setText(currentVendor.getGuid());
            f_name.setText(currentVendor.getName());
            f_email.setText(currentVendor.getEmail());
        } else {
            currentVendor = new VendorModel(WindowNavigator.db(), true);
            UUID thisId = UUID.randomUUID();
            String guid = thisId.toString();
            currentVendor.setGuid(guid);
            f_id.setText(guid);
        }
    }
    public VendorUpdate(int mode, VendorModel model){
        super(mode == MODE_CREATE ? "Thêm đối tác" : mode == MODE_UPDATE ? "Chỉnh sửa thông tin đối tác" : "Xem thông tin đối tác");
        f_title.setText(getTitle());
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        final VendorUpdate vu = this;

        if (mode == MODE_CREATE || mode == MODE_UPDATE) selectedMode = mode;
        else selectedMode = MODE_VIEW;
        if (model != null)
            currentVendor = model.duplicate();
        applyMode();
        displayInfo();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finisher();
            }
        });
        b_return.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        b_insert.addActionListener(e -> new Thread(() -> {
            synchronized (returnLock){
//                if (returnLock.get()) return;
                currentVendor.setName(f_name.getText());
                currentVendor.setEmail(f_email.getText());
                if (currentVendor.getName().isEmpty() || currentVendor.getEmail().isEmpty()){
                    JOptionPane.showMessageDialog(vu, "Thông tin chưa đầy đủ");
                    return;
                }
                b_insert.setEnabled(false);
                b_return.setEnabled(false);
                final FirestoreQuery updateVendor;
                if (selectedMode == MODE_CREATE){
                    updateVendor = new FirestoreQuery(FirestoreQuery.QueryType.CREATE_DOCUMENT)
                            .onCollection("vendors");
                } else {
                    updateVendor = new FirestoreQuery(FirestoreQuery.QueryType.PATCH_DOCUMENT)
                            .onCollection("vendors").onDocument(currentVendor.getDocumentName())
                            .documentExisted(true).updateMask("email").updateMask("name");
                }
                VendorCollection col = new VendorCollection(WindowNavigator.db(), true, updateVendor);
                col.serializeSingle(currentVendor);
                try {
                    col.evaluate();
                    changed.set();
                } catch (FirestoreModelException ex){
                    if (ex.getClass() == NoConnectionException.class){
                        JOptionPane.showMessageDialog(vu, "Kết nối thất bại: Không có Internet");
                    } else if (ex.getClass() == QueryRefusedException.class) {
                        JOptionPane.showMessageDialog(vu, "Kết nối thất bại: Yêu cầu bị từ chối");
                    }
                    L.log("VendorUpdate", ex.toString());
                }
                returnLock.set();
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }
        }).start());
    }
    public boolean run(){
        returnLock.waitToFinish();
        return changed.get();
    }
}

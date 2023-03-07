package ui.main_window.rent_checkout;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.FirestoreDocument;
import com.cycastic.javabase.firestore.FirestoreQuery;
import com.cycastic.javabase.firestore.FirestoreTaskReceiver;
import com.cycastic.javabase.firestore.FirestoreTaskResult;
import entry.WindowNavigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

public class RentCheckout extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(600, 400);
    public static final Dimension CAPSULE_SIZE = new Dimension(131, 191);
    private JPanel MainPanel;
    private JLabel capsule_label;
    private JLabel f_name;
    private JButton b_return;
    private JButton b_accept;
    private JLabel f_director;
    private JLabel f_duration;
    private JLabel f_age;
    private JLabel f_stock;
    private JLabel f_year;
    private JLabel f_category;

    private final FirestoreDocument dvd_detail;
    private final boolean rented;
    private final SafeFlag returnLock = new SafeFlag(false);
    private final Object submitLock = new Object();
    private final SafeFlag allowed = new SafeFlag(false);
    private boolean result = false;


    public RentCheckout(FirestoreDocument details, Image capsule_art, boolean has_rented){
        super("Thuê DVD");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        dvd_detail = details;
        rented = has_rented;
        RentCheckout rc = this;

        final String duration_txt = "%dh %dm"; long _f, _h, _m;
        Map<String, Object> fields = dvd_detail.getFields();
        final String rating_text  = "%s-%d"; long rated_age = (Long) fields.get("rated_age");
        _f = (Long)fields.get("duration");
        _h = _f / 60;
        _m = _f - (60 * _h);
        StringBuilder category_str = new StringBuilder();
        java.util.List<Object> _category = ((List<Object>)fields.get("category"));
        for (int i = 0, s = _category.size(); i < s && i < 2; i++){
            if (i > 0) category_str.append(" • ");
            category_str.append(_category.get(i).toString());
        }
        String imdb_link = fields.get("imdb_link").toString();
        final ImageIcon capsule_icon;
        if (capsule_art != null) capsule_icon = new ImageIcon(capsule_art.getScaledInstance(CAPSULE_SIZE.width, CAPSULE_SIZE.height, Image.SCALE_SMOOTH));
        else capsule_icon = null;

        f_name.setText(fields.get("name").toString());
        f_director.setText(fields.get("director").toString());
        f_duration.setText(duration_txt.formatted(_h, _m));
        f_age.setText(rating_text.formatted(fields.get("rated_type").toString(), rated_age));
        f_stock.setText("...");
        f_year.setText(fields.get("year").toString());
        f_category.setText(category_str.toString());
        new Thread(() -> {
            capsule_label.setIcon(capsule_icon);
            FirestoreQuery fetch_all_renting = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("users").where("renting", FirestoreQuery.Operator.EQUAL, imdb_link);
            WindowNavigator.db().query(fetch_all_renting, new FirestoreTaskReceiver() {
                @Override
                public void connectionFailed() {
                    f_stock.setText("<Kết nối thất bại>");
                    allowed.clear();
                }
                @Override
                public void requestFailed(int i, Map<String, String> map, String s) {
                    f_stock.setText("<Không rõ>");
                    allowed.clear();
                }
                @Override
                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                    Long base_stock = (Long)fields.get("stock");
                    long current = base_stock - firestoreTaskResult.getDocuments().size();
                    long displaying = Math.max(current, 0);
                    f_stock.setText(Long.toString(displaying));
                    if (displaying > 0) allowed.set();
                    else allowed.clear();
                }
            });
        }).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!returnLock.get()) returnLock.set();
                rc.dispose();
            }
        });
        b_return.addActionListener(e -> {
            result = false;
            returnLock.set();
            rc.dispose();
        });
        b_accept.addActionListener(e -> {
            synchronized (submitLock){
                if (!allowed.get()){
                    JOptionPane.showMessageDialog(rc, "Không thể kiểm tra được số lượng tồn kho");
                    return;
                }
                int re;
                if (rented) {
                    re = JOptionPane.showConfirmDialog(rc, "Bạn đã thuê một DVD khác. Có muốn hoàn trả DVD đó và thuê đĩa này không?");
                } else {
                    re = JOptionPane.showConfirmDialog(rc, "Xác nhận thuê DVD?");
                }
                if (re != JOptionPane.YES_OPTION) return;
                result = true;
                returnLock.set();
                rc.dispose();
            }
        });
    }

    public boolean run(){
        while (!returnLock.get()) { continue; }
        return result;
    }

}

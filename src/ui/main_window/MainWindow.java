package ui.main_window;

import com.cycastic.javabase.collection.ReferencesList;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;
import ui.auth_window.user_create.UserCreateForm;
import ui.main_window.rent_checkout.RentCheckout;
import ui.main_window.store_filter.StoreFilterForm;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class MainWindow extends JFrame {
    private JPanel MainPanel;
    private JButton b_prev;
    private JButton b_next;
    private JLabel l_sf_page_count;
    private JButton bc_0;
    private JPanel pc_0;
    private JLabel lc_name0;
    private JLabel lc__dur0;
    private JLabel lc_year0;
    private JPanel c0;
    private JPanel pc_1;
    private JButton bc_1;
    private JPanel c1;
    private JLabel lc_name1;
    private JLabel lc__dur1;
    private JLabel lc_year1;
    private JPanel c2;
    private JPanel pc_2;
    private JButton bc_2;
    private JLabel lc_name2;
    private JLabel lc__dur2;
    private JLabel lc_year2;
    private JPanel pc_3;
    private JButton bc_3;
    private JLabel lc_name3;
    private JLabel lc__dur3;
    private JLabel lc_year3;
    private JPanel c3;
    private JPanel c4;
    private JPanel pc_4;
    private JButton bc_4;
    private JLabel lc_name4;
    private JLabel lc__dur4;
    private JLabel lc_year4;
    private JScrollPane main_scroll_pane;
    private JLabel lc_0;
    private JLabel lc_1;
    private JLabel lc_2;
    private JLabel lc_3;
    private JLabel lc_4;
    private JButton b_refresh;
    private JButton filter0;
    private JButton filter1;
    private JScrollPane scroll_pane_2;
    private JLabel user_pfp;
    private JTextField uf_username;
    private JTextField uf_email;
    private JTextField uf_dob;
    private JLabel u_capsule;
    private JLabel u_name;
    private JLabel u_duration;
    private JLabel u_year;
    private JButton ub_return;
    private JTextField uf_rented;
    private JPanel up_renting;
    private JLabel ul_renting;

    public static final Dimension WINDOW_SIZE  = new Dimension(900, 400);
    public static final Dimension CAPSULE_SIZE = new Dimension(195, 285);
    public static final Dimension PFP_SIZE = new Dimension(200, 200);

    private final List<FirestoreDocument> dvd_list = new ArrayList<>();
    private FirestoreDocument user_profile = null;
    private final List<Image> capsule_list = new ArrayList<>();
    private int store_current_page = 1;
    private int store_max_page = 0;
    private final ReferencesList<StoreFilterForm.StoreFilterSettings> filters;
    private final Object storeRefreshSynchronizer = new Object();
    private final Object userRefreshSynchronizer = new Object();
    private final Object rentHandlerSynchronizer = new Object();
    private final Object filtersLock = new Object();
    private class StoreRefreshHandler {
        private final MainWindow mw;
        public StoreRefreshHandler(MainWindow mw){
            this.mw = mw;
        }
        public void run(){
            synchronized (storeRefreshSynchronizer) {
                mw.lock_input();
                dvd_list.clear();
                capsule_list.clear();
                FirestoreQuery fetch_list = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY).from("dvds_info");
                filters.forEach(storeFilterElement -> {
                    if (storeFilterElement.getValue() != null)
                        fetch_list.where(storeFilterElement.getValue().field, storeFilterElement.getValue().operator,
                                storeFilterElement.getValue().value);
                });
                update_store_page(0);
                WindowNavigator.db().query(fetch_list, new FirestoreTaskReceiver() {
                    @Override
                    public void connectionFailed() {
                        JOptionPane.showMessageDialog(mw, "Không thể kết nối đến máy chủ Firestore. Hãy kiểm tra lại kết nối internet");
                        mw.unlock_input();
                    }
                    @Override
                    public void requestFailed(int i, Map<String, String> map, String s) {
                        JOptionPane.showMessageDialog(mw, "Đã xảy ra lỗi khi tìm kiếm thông tin. Xin hãy liên hệ cho Khánh Nam.");
                        mw.unlock_input();
                    }
                    @Override
                    public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                        synchronized (storeRefreshSynchronizer) {
                            for (Map.Entry<String, FirestoreDocument> E : firestoreTaskResult.getDocuments().entrySet()){
                                FirestoreDocument doc = E.getValue();
                                dvd_list.add(doc);
                            }
                            update_store_data_text(1);
                            for (FirestoreDocument doc : dvd_list){
                                try {
                                    URL capsule_url = new URL(doc.getFields().get("capsule_art").toString());
                                    BufferedImage img = ImageIO.read(capsule_url);
                                    capsule_list.add(img.getScaledInstance(CAPSULE_SIZE.width, CAPSULE_SIZE.height, Image.SCALE_SMOOTH));
                                } catch (IOException e){
                                    capsule_list.add(null);
                                }
                            }
                            int total = dvd_list.size() / 5;
                            store_max_page = (dvd_list.size() - (total * 5)) > 0 ? total + 1 : total;
                            update_store_page(1);
        //                    update_store_capsules(1);
                            mw.unlock_input();
                        }
                    }
                });
            }
        }
    }
    private class UserRefreshHandler {
        private final MainWindow mw;
        public UserRefreshHandler(MainWindow mw){
            this.mw = mw;
        }
        public void run(){
            String uid = WindowNavigator.auth().getAuthWrapper().getAuthToken().getLocalId();
            FirestoreQuery fetchUser = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("users").where("uid", FirestoreQuery.Operator.EQUAL, uid).relate(FirestoreQuery.Operator.AND);
            WindowNavigator.db().query(fetchUser, new FirestoreTaskReceiver() {
                @Override
                public void connectionFailed() {
                    JOptionPane.showMessageDialog(mw, "Không thể tra được thông tin người dung: không có kết nối internet");
                }
                @Override
                public void requestFailed(int i, Map<String, String> map, String s) {
                    JOptionPane.showMessageDialog(mw, "Không thể tra được thông tin người dung: yêu cầu thất bại");
                }
                @Override
                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                    synchronized (userRefreshSynchronizer){
                        Map<String, FirestoreDocument> docs = firestoreTaskResult.getDocuments();
                        for (Map.Entry<String, FirestoreDocument> E : docs.entrySet()){
                            user_profile = E.getValue();
                            Date dob = UserCreateForm.unixToDate((Long)user_profile.getFields().get("dob"));
                            uf_username.setText(user_profile.getFields().get("username").toString());
                            uf_dob.setText(String.valueOf(UserCreateForm.getAgeByDob(dob)));
                            uf_rented.setText(user_profile.getFields().get("rented").toString());
                            try {
                                URL pfp_url = new URL(user_profile.getFields().get("pfp").toString());
                                BufferedImage img = ImageIO.read(pfp_url);
                                Image avatar = img.getScaledInstance(PFP_SIZE.width, PFP_SIZE.height, Image.SCALE_SMOOTH);
                                user_pfp.setIcon(new ImageIcon(avatar));
                            } catch (Exception ignored) {}
                            //-----------------------------------------------------
                            String renting_imdb_link = user_profile.getFields().get("renting").toString();
                            if (renting_imdb_link.isEmpty()) {
                                update_store_component(null, up_renting, u_name, u_duration, u_year);
                                update_store_panel(u_capsule, null);
                                ul_renting.setVisible(true);
                                break;
                            }
                            FirestoreQuery fetch_renting_metadata = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                                    .from("dvds_info").where("imdb_link", FirestoreQuery.Operator.EQUAL, renting_imdb_link);
                            WindowNavigator.db().query(fetch_renting_metadata, new FirestoreTaskReceiver() {
                                @Override
                                public void connectionFailed() {
                                    update_store_component(null, up_renting, u_name, u_duration, u_year);
                                    update_store_panel(u_capsule, null);
                                    ul_renting.setVisible(true);
                                    JOptionPane.showMessageDialog(mw, "Không có kết nối mạng");
                                }
                                @Override
                                public void requestFailed(int i, Map<String, String> map, String s) {
                                    update_store_component(null, up_renting, u_name, u_duration, u_year);
                                    update_store_panel(u_capsule, null);
                                    ul_renting.setVisible(true);
                                    JOptionPane.showMessageDialog(mw, "Yêu cầu thất bại. Không thể tìm thấy DVD đang thuê");
                                }
                                @Override
                                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                                    Map<String, FirestoreDocument> docs = firestoreTaskResult.getDocuments();
                                    FirestoreDocument dvd_metadata = null;
                                    for (Map.Entry<String, FirestoreDocument> E : docs.entrySet()){
                                        dvd_metadata = E.getValue();
                                        break;
                                    }
                                    if (dvd_metadata == null){
                                        update_store_component(null, up_renting, u_name, u_duration, u_year);
                                        update_store_panel(u_capsule, null);
                                        ul_renting.setVisible(true);
                                        return;
                                    }
                                    final FirestoreDocument doc = dvd_metadata;
                                    new Thread(() -> {
                                        try {
                                            URL capsule_url = new URL(doc.getFields().get("capsule_art").toString());
                                            BufferedImage img = ImageIO.read(capsule_url);
                                            update_store_panel(u_capsule, img.getScaledInstance(CAPSULE_SIZE.width, CAPSULE_SIZE.height, Image.SCALE_SMOOTH));
                                        } catch (Exception ignored){
                                            update_store_panel(u_capsule, null);
                                        }
                                    }).start();
                                    ul_renting.setVisible(false);
                                    update_store_component(dvd_metadata, up_renting, u_name, u_duration, u_year);
                                }
                            });

                            // Người dùng với uid đã cho chỉ có 1
                            break;
                        }
                    }
                }
            });
        }
    }
    private class RentHandler {
        private final MainWindow mw;
        public RentHandler(MainWindow mw){
            this.mw = mw;
        }
        public void run(int idx){
            if (dvd_list.isEmpty()) return;
            final FirestoreDocument pf_doc;
            synchronized (userRefreshSynchronizer){
                if (user_profile == null){
                    JOptionPane.showMessageDialog(mw, "Không thể đọc được thông tin người dùng");
                    return;
                }
                else {
                    pf_doc = user_profile;
                }
            }
            mw.lock_input();
            final int anchor_index = (store_current_page - 1) * 5;
            final int curr = anchor_index + idx;
            new Thread(() -> {
                synchronized (rentHandlerSynchronizer){
                    FirestoreDocument doc = dvd_list.get(curr);
                    Image capsule = capsule_list.get(curr);
                    String imdb_link = doc.getFields().get("imdb_link").toString();
                    if (Objects.equals(imdb_link, pf_doc.getFields().get("renting"))) {
                        JOptionPane.showMessageDialog(mw, "Bạn đã thuê DVD này!");
                        mw.unlock_input();
                        return;
                    }
                    int age = UserCreateForm.getAgeByDob(UserCreateForm.unixToDate((Long)pf_doc.getFields().get("dob")));
                    boolean has_rented = !(pf_doc.getFields().get("renting").toString().isEmpty());
                    boolean accepted = new RentCheckout(doc, capsule, has_rented).run();
                    if (!accepted) {
                        mw.unlock_input();
                        return;
                    }
                    if (age < (Long)doc.getFields().get("rated_age")) {
                        JOptionPane.showMessageDialog(mw, "Bạn không đủ tuổi để thuê phim này.");
                        mw.unlock_input();
                        return;
                    }
                    long rented = (Long)pf_doc.getFields().get("rented");
                    Map<String, Object> update_content = new HashMap<>();
                    update_content.put("renting", imdb_link);
                    update_content.put("rented", rented + 1);
                    FirestoreQuery patch_renting = new FirestoreQuery(FirestoreQuery.QueryType.PATCH_DOCUMENT)
                            .onCollection("users").onDocument(pf_doc.getFields().get("uid").toString())
                            .update(update_content).documentExisted(true).updateMask("renting")
                            .updateMask("rented");
                    WindowNavigator.db().query(patch_renting, new FirestoreTaskReceiver() {
                        @Override
                        public void connectionFailed() {
                            JOptionPane.showMessageDialog(mw, "Không thể thuê được DVD: Kết nối thất bại");
                            mw.unlock_input();
                        }
                        @Override
                        public void requestFailed(int i, Map<String, String> map, String s) {
                            JOptionPane.showMessageDialog(mw, "Không thể thuê được DVD: Yêu cầu thất bại");
                            mw.unlock_input();
                        }

                        @Override
                        public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                            JOptionPane.showMessageDialog(mw, "Thuê DVD thành công");
                            mw.unlock_input();
                            new UserRefreshHandler(mw).run();
                        }
                    });
                }
            }).start();
//            mw.unlock_input();
        }
    }

    static private void update_store_component(FirestoreDocument doc, JPanel host, JLabel title, JLabel duration, JLabel year){
        if (doc == null || doc.getFields().isEmpty()) {
            title.setText("");
            duration.setText("");
            year.setText("");
            host.setVisible(false);
        } else {
            Map<String, Object> fields = doc.getFields();
            String duration_txt = "%dh %dm"; long _f, _h, _m;
            String rating_text  = "%s-%d"; long rated_age = (Long) fields.get("rated_age");
            _f = (Long)fields.get("duration");
            _h = _f / 60;
            _m = _f - (60 * _h);

            StringBuilder category_str = new StringBuilder();
            List<Object> _category = ((List<Object>)fields.get("category"));
            for (int i = 0, s = _category.size(); i < s && i < 2; i++){
                if (i > 0) category_str.append(" • ");
                category_str.append(_category.get(i).toString());
            }

            title.setText(fields.get("name").toString());
            duration.setText(duration_txt.formatted(_h, _m) + " | "
                    + rating_text.formatted(fields.get("rated_type").toString(), rated_age)
                    + " | " + fields.get("year").toString());
            year.setText(category_str.toString());
            host.setVisible(true);
        }
    }
    static private void update_store_panel(JLabel capsule, Image capsule_img){
        if (capsule_img == null) {
            capsule.setIcon(new ImageIcon());
        } else {

            capsule.setIcon(new ImageIcon(capsule_img));
        }
    }
    private FirestoreDocument get_dvd_info(int index){
        if (index >= dvd_list.size() || index < 0) return null;
        return dvd_list.get(index);
    }
    private Image get_capsule(int index){
        if (index >= capsule_list.size() || index < 0) return null;
        return capsule_list.get(index);
    }
    private void update_store_data_text(int new_page){
        int relative_index = 0, curr;
        int anchor_index = (new_page - 1) * 5;
        FirestoreDocument doc;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c0, lc_name0, lc__dur0, lc_year0);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c1, lc_name1, lc__dur1, lc_year1);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c2, lc_name2, lc__dur2, lc_year2);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c3, lc_name3, lc__dur3, lc_year3);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c4, lc_name4, lc__dur4, lc_year4);


        l_sf_page_count.setText("%d/%d".formatted(new_page, store_max_page));
    }
    private void update_store_capsules(int new_page){
        int anchor_index = (new_page - 1) * 5;
        update_store_panel(lc_0, get_capsule(anchor_index));
        update_store_panel(lc_1, get_capsule(anchor_index + 1));
        update_store_panel(lc_2, get_capsule(anchor_index + 2));
        update_store_panel(lc_3, get_capsule(anchor_index + 3));
        update_store_panel(lc_4, get_capsule(anchor_index + 4));
    }
    private void update_store_page(int new_page){
        update_store_data_text(new_page);
        update_store_capsules(new_page);

        l_sf_page_count.setText("%d/%d".formatted(new_page, store_max_page));
    }

    private void fetch_dvd_list(){
        new StoreRefreshHandler(this).run();
    }
    private void fetch_user_info(){
        new UserRefreshHandler(this).run();
    }
    private void rent_at(int index){
        new RentHandler((this)).run(index);
    }

    public void change_input(boolean state){
        b_next.setEnabled(state);
        b_prev.setEnabled(state);
        bc_0.setEnabled(state);
        bc_1.setEnabled(state);
        bc_2.setEnabled(state);
        bc_3.setEnabled(state);
        bc_4.setEnabled(state);
        b_refresh.setEnabled(state);
        filter0.setEnabled(state);
//        filter1.setEnabled(state);
    }

    public void lock_input(){
        change_input(false);
    }
    public void unlock_input(){
        change_input(true);
    }

    private static int clamp(int val, int min, int max){
        return val < min ? min : Math.min(val, max);
    }
    private boolean isUpdatingFilters = false;
    private void addFilter(int index, JButton bFilter){

        StoreFilterForm.StoreFilterSettings settings = new StoreFilterForm(index).getFilterSettings();
        if (Objects.equals(settings.field, "stock")) return;
        if (Objects.equals(settings.field, "category")) {
            JOptionPane.showMessageDialog(this, "Javabase không còn hỗ trợ tính năng này ('in' operator)");
            return;
        }
        int i = 0;
        for (ReferencesList.Element<StoreFilterForm.StoreFilterSettings> E = filters.first();
                E != null; E = E.next()){
            if (i == index) {
                E.setValue(settings);
                break;
            }
            i += 1;
        }
        bFilter.setText(settings.field);
    }
    private void removeFilter(int index, JButton bFilter){
        int i = 0;
        for (ReferencesList.Element<StoreFilterForm.StoreFilterSettings> E = filters.first();
             E != null; E = E.next()){
            if (i == index) {
                E.setValue(null);
                break;
            }
            i += 1;
        }
        bFilter.setText("+");
    }
    private void filterHandler(int index, JButton bFilter){
        if (isUpdatingFilters) return;
        new Thread(() -> {
            synchronized (filtersLock){
                isUpdatingFilters = true;
                if (Objects.equals(bFilter.getText(), "+")) addFilter(index, bFilter);
                else removeFilter(index, bFilter);
                isUpdatingFilters = false;
            }
        }).start();
    }

    public MainWindow(){
        super("Thuê DVD OwO");

        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        main_scroll_pane.getVerticalScrollBar().setUnitIncrement(16);
        scroll_pane_2.getVerticalScrollBar().setUnitIncrement(16);

        MainWindow mw = this;
        filters = new ReferencesList<>();
        filters.pushBack(new StoreFilterForm.StoreFilterSettings());
        for (int i = 0; i < 2; i++){
            filters.pushBack(null);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                WindowNavigator.closeWindow(mw);
            }
        });
        b_prev.addActionListener(e -> {
            store_current_page = clamp(store_current_page - 1, 1, store_max_page);
            update_store_page(store_current_page);
        });
        b_next.addActionListener(e -> {
            store_current_page = clamp(store_current_page + 1, 1, store_max_page);
            update_store_page(store_current_page);
        });
        b_refresh.addActionListener(e -> fetch_dvd_list());
        filter0.addActionListener(e -> {
            filterHandler(0, filter0);
        });
        filter1.addActionListener(e -> {
            filterHandler(1, filter1);
        });
        fetch_dvd_list();
        fetch_user_info();
        bc_0.addActionListener(e -> {
            rent_at(0);
        });
        bc_1.addActionListener(e -> {
            rent_at(1);
        });
        bc_2.addActionListener(e -> {
            rent_at(2);
        });
        bc_3.addActionListener(e -> {
            rent_at(3);
        });
        bc_4.addActionListener(e -> {
            rent_at(4);
        });
        ub_return.addActionListener(e -> {
            int ret = JOptionPane.showConfirmDialog(mw, "Xác nhận trả đĩa?");
            if (ret != JOptionPane.YES_OPTION) return;
            final FirestoreDocument pf_doc;
            synchronized (userRefreshSynchronizer){
                pf_doc = user_profile;
            }
            if (pf_doc == null){
                JOptionPane.showMessageDialog(mw, "Không thể đọc được cài đặt người dùng");
                return;
            }
            Map<String, Object> update_content = new HashMap<>();
            update_content.put("renting", "");
            FirestoreQuery return_query = new FirestoreQuery(FirestoreQuery.QueryType.PATCH_DOCUMENT)
                    .onCollection("users").onDocument(pf_doc.getFields().get("uid").toString())
                    .update(update_content).documentExisted(true).updateMask("renting");
            WindowNavigator.db().query(return_query, new FirestoreTaskReceiver() {
                @Override
                public void connectionFailed() {
                    JOptionPane.showMessageDialog(mw, "Không có kết nối Internet");
                }
                @Override
                public void requestFailed(int i, Map<String, String> map, String s) {
                    JOptionPane.showMessageDialog(mw, "Trả đĩa không thành công");
                }
                @Override
                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                    JOptionPane.showMessageDialog(mw, "Trả đĩa thành công");
                    new UserRefreshHandler(mw).run();
                }
            });
        });
    }
}

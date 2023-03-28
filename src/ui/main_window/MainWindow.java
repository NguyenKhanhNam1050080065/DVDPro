package ui.main_window;

import com.cycastic.javabase.collection.ReferencesList;
import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;
import misc.L;
import models.*;
import ui.auth_window.user_create.UserCreateForm;
import ui.main_window.management_tools.AddDVD;
import ui.main_window.management_tools.DVDInfoViewer;
import ui.main_window.management_tools.LedgerViewer;
import ui.main_window.management_tools.VendorsViewer;
import ui.main_window.store_filter.StoreFilterForm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private JButton b_vendors;
    private JList<String> transactionList;
    private JRadioButton rb_time;
    private JRadioButton rb_type;
    private JRadioButton rb_amount;
    private JButton b_transaction_sort;
    private JScrollPane transactionScrollPane;
    private JCheckBox cb_ascending;
    private JButton b_add_dvd;
    private ButtonGroup bg;

    public static final Dimension WINDOW_SIZE  = new Dimension(900, 400);
    public static final Dimension CAPSULE_SIZE = new Dimension(200, 300);
    public static final Dimension PFP_SIZE = new Dimension(200, 200);

    private final List<DVDModel> dvd_models = new ArrayList<>();
    private FirestoreDocument user_profile = null;
    private final List<Image> capsule_list = new ArrayList<>();
    private int store_current_page = 1;
    private int store_max_page = 0;
    private final ReferencesList<StoreFilterForm.StoreFilterSettings> filters;
    private final DefaultListModel<String> transactionModel;
    private final java.util.List<LedgerModel> transactionIds = new ArrayList<>();
    private final Object storeRefreshSynchronizer = new Object();
    private final Object userRefreshSynchronizer = new Object();
    private final Object rentHandlerSynchronizer = new Object();
    private final Object transactionListLock = new Object();
    private final Object filtersLock = new Object();
    private final SafeFlag isAdmin = new SafeFlag(false);

    private void createUIComponents() {
        // TODO: place custom component creation code here
        transactionList = new JList<>(new DefaultListModel<>());
        bg = new ButtonGroup();
        rb_amount = new JRadioButton();
        rb_time = new JRadioButton();
        rb_type = new JRadioButton();
        bg.add(rb_amount);
        bg.add(rb_time);
        bg.add(rb_type);
        rb_time.setSelected(true);
    }

    private class StoreRefreshHandler {
        private final MainWindow mw;
        public StoreRefreshHandler(MainWindow mw){
            this.mw = mw;
        }
        public void run(){
            synchronized (storeRefreshSynchronizer) {
                mw.lockInput();
                dvd_models.clear();
                capsule_list.clear();
                //-------------------------------------------------------
                DVDCollection collection = new DVDCollection(WindowNavigator.db(), true).setRead();
                filters.forEach(storeFilterSettingsElement -> {
                    if (storeFilterSettingsElement.getValue() != null)
                        collection.getBaseQuery().where(storeFilterSettingsElement.getValue().field,
                                storeFilterSettingsElement.getValue().operator, storeFilterSettingsElement.getValue().value);
                });
                collection.getBaseQuery().orderBy("stock", FirestoreQuery.Direction.DESCENDING);
                update_store_page(0);
                try{
                    Map<String, DVDModel> dvds = collection.getDocuments();
                    for (Map.Entry<String, DVDModel> E : dvds.entrySet()){
                        dvd_models.add(E.getValue());
                    }
                    update_store_data_text(1);
                    for (DVDModel doc : dvd_models){
                        String capsuleLink = doc.getCapsuleArt();
                        BufferedImage img = WindowNavigator.capsules().fetch(capsuleLink);
                        if (img == null){
                            capsule_list.add(null);
                        } else {
                            capsule_list.add(img.getScaledInstance(CAPSULE_SIZE.width, CAPSULE_SIZE.height, Image.SCALE_SMOOTH));
                        }
                    }
                    int total = dvd_models.size() / 5;
                    store_max_page = (dvd_models.size() - (total * 5)) > 0 ? total + 1 : total;
                    update_store_page(1);
                } catch (FirestoreModelException e){
                    if (e.getClass() == NoConnectionException.class){
                        JOptionPane.showMessageDialog(mw, "Không thể kết nối đến máy chủ Firestore. Hãy kiểm tra lại kết nối internet");
                    } else if (e.getClass() == QueryRefusedException.class){
                        JOptionPane.showMessageDialog(mw, "Đã xảy ra lỗi khi tìm kiếm thông tin. Xin hãy liên hệ cho Khánh Nam.");
                    }
                    L.log("MainWindow", e.toString());
                } finally {
                    mw.unlockInput();
                }
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
                            //-------------------------------------------------------------------------------------
                            final SafeFlag is_mod_fetch_finished = new SafeFlag(false);
                            FirestoreQuery fetch_moderation_status = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                                    .from("users_status").where("uid", FirestoreQuery.Operator.EQUAL, user_profile.getFields().get("uid").toString());
                            WindowNavigator.db().query(fetch_moderation_status, new FirestoreTaskReceiver() {
                                @Override
                                public void connectionFailed() {
                                    isAdmin.clear();
                                    is_mod_fetch_finished.set();
                                }
                                @Override
                                public void requestFailed(int i, Map<String, String> map, String s) {
                                    isAdmin.clear();
                                    is_mod_fetch_finished.set();
                                }
                                @Override
                                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                                    if (firestoreTaskResult.getDocuments().isEmpty()) isAdmin.clear();
                                    else {
                                        for (Map.Entry<String, FirestoreDocument> E : firestoreTaskResult.getDocuments().entrySet()){
                                            if ((Boolean) E.getValue().getFields().get("isAdmin")){
                                                isAdmin.set();
                                            } else isAdmin.clear();
                                            break;
                                        }
                                    }
                                    is_mod_fetch_finished.set();
//                                    uf_moderation.setText(String.valueOf(isAdmin.get()));
                                    //-------------------------------------------------------------------------------------
                                }
                            });
                            is_mod_fetch_finished.waitToFinish();
                            break;
                        }
                    }
                }
            });
        }
    }
    private class DetailHandler {
        private final MainWindow mw;
        public DetailHandler(MainWindow mw){
            this.mw = mw;
        }
        public void run(int idx){
            if (dvd_models.isEmpty()) return;
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
            mw.lockInput();
            final int anchor_index = (store_current_page - 1) * 5;
            final int curr = anchor_index + idx;
            new Thread(() -> {
                synchronized (rentHandlerSynchronizer){
                    DVDModel doc = dvd_models.get(curr);
                    Image capsule = capsule_list.get(curr);
                    String imdb_link = doc.getImdbLink();
                    String uid = pf_doc.getFields().get("uid").toString();
                    int age = UserCreateForm.getAgeByDob(UserCreateForm.unixToDate((Long)pf_doc.getFields().get("dob")));
                    final DVDInfoViewer rc = new DVDInfoViewer(doc, capsule, isAdmin.get());
                    boolean accepted = rc.run();
                    final boolean modified = rc.isModified();
                    mw.unlockInput();
                }
            }).start();
        }
    }

    @Deprecated
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
    static private void updateStoreComponent(DVDModel doc, JPanel host, JLabel title, JLabel duration, JLabel year){
        if (doc == null){
            title.setText("");
            duration.setText("");
            year.setText("");
            host.setVisible(false);
            return;
        }
        String duration_txt = "%dh %dm"; long _f, _h, _m;
        String rating_text  = "%s-%d"; long rated_age = doc.getRatedAge();
        _f = doc.getDuration();
        _h = _f / 60;
        _m = _f - (60 * _h);
        StringBuilder category_str = new StringBuilder();
        List<String> _category = (doc.getCategory());
        for (int i = 0, s = _category.size(); i < s && i < 2; i++){
            if (i > 0) category_str.append(" • ");
            category_str.append(_category.get(i));
        }
        title.setText(doc.getName());
        duration.setText(duration_txt.formatted(_h, _m) + " | "
                + rating_text.formatted(doc.getRatedAge(), rated_age)
                + " | " + doc.getYear());
        year.setText(category_str.toString());
        host.setVisible(true);
    }
    static private void update_store_panel(JLabel capsule, Image capsule_img){
        if (capsule_img == null) {
            capsule.setIcon(new ImageIcon());
        } else {

            capsule.setIcon(new ImageIcon(capsule_img));
        }
    }
    private DVDModel get_dvd_model(int index){
        if (index >= dvd_models.size() || index < 0) return null;
        return dvd_models.get(index);
    }
    private Image get_capsule(int index){
        if (index >= capsule_list.size() || index < 0) return null;
        return capsule_list.get(index);
    }
    private void update_store_data_text(int new_page){
        int relative_index = 0, curr;
        int anchor_index = (new_page - 1) * 5;
        DVDModel doc;

        curr = anchor_index + relative_index;
        doc = get_dvd_model(curr);
        updateStoreComponent(doc, c0, lc_name0, lc__dur0, lc_year0);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_model(curr);
        updateStoreComponent(doc, c1, lc_name1, lc__dur1, lc_year1);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_model(curr);
        updateStoreComponent(doc, c2, lc_name2, lc__dur2, lc_year2);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_model(curr);
        updateStoreComponent(doc, c3, lc_name3, lc__dur3, lc_year3);
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_model(curr);
        updateStoreComponent(doc, c4, lc_name4, lc__dur4, lc_year4);


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
        new Thread(()-> new StoreRefreshHandler(this).run()).start();
    }
    private void fetch_user_info(){
        new UserRefreshHandler(this).run();
    }
    private void viewDetailAt(int index){
        new DetailHandler((this)).run(index);
    }

    public void changeInputState(boolean state){
        b_vendors.setEnabled(state && isAdmin.get());
        b_add_dvd.setEnabled(state && isAdmin.get());
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
    public void changeTransactionListState(boolean state){
        transactionList.setEnabled(state);
        rb_amount.setEnabled(state);
        rb_type.setEnabled(state);
        rb_time.setEnabled(state);
        cb_ascending.setEnabled(state);
        b_transaction_sort.setEnabled(state);
    }

    public void lockInput(){
        changeInputState(false);
    }
    public void unlockInput(){
        changeInputState(true);
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
                lockInput();
                isUpdatingFilters = true;
                if (Objects.equals(bFilter.getText(), "+")) addFilter(index, bFilter);
                else removeFilter(index, bFilter);
                isUpdatingFilters = false;
                unlockInput();
            }
        }).start();
    }
    private void refreshTransactionList(){
        synchronized (transactionListLock){
            changeTransactionListState(false);
            transactionModel.clear();
            transactionIds.clear();
        }
        new Thread(() -> {
            synchronized (transactionListLock){
                final int selectedFilter;
                if (rb_type.isSelected()) selectedFilter = 1;
                else if (rb_amount.isSelected()) selectedFilter = 2;
                else selectedFilter = 0;
                FirestoreQuery getTransactions = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                        .from("ledger")
                        .orderBy(selectedFilter == 2 ? "amount" : selectedFilter == 1 ? "isInput" : "time", cb_ascending.isSelected() ? FirestoreQuery.Direction.ASCENDING : FirestoreQuery.Direction.DESCENDING);
                LedgerCollection ledgerCollection = new LedgerCollection(WindowNavigator.db(), true, getTransactions);
                try {
                    Map<String, LedgerModel> ledger = ledgerCollection.getDocuments();
                    for (Map.Entry<String, LedgerModel> E : ledger.entrySet()){
                        LedgerModel model = E.getValue();
                        LocalDateTime transactionTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(model.getTime()), ZoneId.systemDefault());
                        final String textDisplay = "%s %d sản phẩm vào %s".formatted(model.getInput() ? "Nhập kho" : "Xuất kho", model.getAmount(), transactionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        transactionModel.addElement(textDisplay);
                        transactionIds.add(model);
                    }
                } catch (FirestoreModelException ex){
                    if (ex.getClass() == NoConnectionException.class){
                        JOptionPane.showMessageDialog(this, "Yêu cầu thông tin giao dịch thất bại: Không có kết nối Internet");
                    } else if (ex.getClass() == QueryRefusedException.class){
                        JOptionPane.showMessageDialog(this, "Yêu cầu thông tin giao dịch thất bại: Yêu cầu bị từ chối");
                    }
                    L.log("MainWindow", "Transaction request failed: %s".formatted(ex.toString()));
                }

                changeTransactionListState(true);
            }
        }).start();
    }
    private void viewTransaction(){
        new Thread(() -> {
            synchronized (transactionListLock){
                int idx = transactionList.getSelectedIndex();
                if (idx == -1) return;
                changeTransactionListState(false);
                new LedgerViewer(transactionIds.get(idx)).run();
                changeTransactionListState(true);
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
        transactionScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        MainWindow mw = this;
        filters = new ReferencesList<>();
        filters.pushBack(new StoreFilterForm.StoreFilterSettings());
        for (int i = 0; i < 2; i++){
            filters.pushBack(null);
        }

        transactionModel = (DefaultListModel<String>) transactionList.getModel();
        refreshTransactionList();

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
        b_vendors.addActionListener(e -> new Thread(() -> {
            lockInput();
            new VendorsViewer().run();
            unlockInput();
        }).start());
        b_add_dvd.addActionListener(e -> new Thread(() -> {
            lockInput();
            new AddDVD(false, null).run();
            unlockInput();
        }).start());
        b_transaction_sort.addActionListener(e -> refreshTransactionList());
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
            viewDetailAt(0);
        });
        bc_1.addActionListener(e -> {
            viewDetailAt(1);
        });
        bc_2.addActionListener(e -> {
            viewDetailAt(2);
        });
        bc_3.addActionListener(e -> {
            viewDetailAt(3);
        });
        bc_4.addActionListener(e -> {
            viewDetailAt(4);
        });
        transactionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) viewTransaction();
            }
        });
    }
}

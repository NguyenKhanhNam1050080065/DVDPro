package ui.main_window;

import com.cycastic.javabase.collection.ReferencesList;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private JPanel user_info;
    private JButton b_refresh;

    public static final Dimension WINDOW_SIZE  = new Dimension(900, 400);
    public static final Dimension CAPSULE_SIZE = new Dimension(195, 285);

    private final List<FirestoreDocument> dvd_list = new ArrayList<>();
    private final List<Image> capsule_list = new ArrayList<>();
    private int store_current_page = 1;
    private int store_max_page = 0;
    private final ReferencesList<StoreFilter> filters;
    private final Object refreshSynchronizer = new Object();
    private class StoreRefreshHandler {
        private final MainWindow mw;
        public StoreRefreshHandler(MainWindow mw){
            this.mw = mw;
        }
        public void run(){
            mw.lock_input();
            dvd_list.clear();
            capsule_list.clear();
            FirestoreQuery fetch_list = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY).from("dvds_info");
            filters.forEach(storeFilterElement -> {
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
                    for (Map.Entry<String, FirestoreDocument> E : firestoreTaskResult.getDocuments().entrySet()){
                        FirestoreDocument doc = E.getValue();
                        dvd_list.add(doc);
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
                    mw.unlock_input();
                }
            });
        }
    }
    public static class StoreFilter {
        public final String field;
        public final FirestoreQuery.Operator operator;
        public final Object value;
        public StoreFilter(){
            this("stock", FirestoreQuery.Operator.GREATER_THAN, 0);
        }
        public StoreFilter(String field, FirestoreQuery.Operator operator, Object value){
            this.field = field;
            this.operator = operator;
            this.value = value;
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
            String duration_txt = "%dh %dm"; int _f, _h, _m;
            String rating_text  = "%s-%d"; int rated_age = (Integer) fields.get("rated_age");
            _f = (Integer)fields.get("duration");
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
    private void update_store_page(int new_page){
        int relative_index = 0, curr;
        int anchor_index = (new_page - 1) * 5;
        FirestoreDocument doc;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c0, lc_name0, lc__dur0, lc_year0);
        update_store_panel(lc_0, get_capsule(anchor_index + relative_index));
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c1, lc_name1, lc__dur1, lc_year1);
        update_store_panel(lc_1, get_capsule(anchor_index + relative_index));
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c2, lc_name2, lc__dur2, lc_year2);
        update_store_panel(lc_2, get_capsule(anchor_index + relative_index));
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c3, lc_name3, lc__dur3, lc_year3);
        update_store_panel(lc_3, get_capsule(anchor_index + relative_index));
        relative_index +=1 ;

        curr = anchor_index + relative_index;
        doc = get_dvd_info(curr);
        update_store_component(doc, c4, lc_name4, lc__dur4, lc_year4);
        update_store_panel(lc_4, get_capsule(anchor_index + relative_index));


        l_sf_page_count.setText("%d/%d".formatted(new_page, store_max_page));
    }

    private void fetch_dvd_list(){
        synchronized (refreshSynchronizer) {
            new StoreRefreshHandler(this).run();
        }
    }

    public void change_input(boolean state){
        b_next.setEnabled(state);
        b_prev.setEnabled(state);
        bc_0.setEnabled(state);
        bc_1.setEnabled(state);
        bc_2.setEnabled(state);
        bc_3.setEnabled(state);
        bc_4.setEnabled(state);
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

    public MainWindow(){
        super("Thuê DVD OwO");

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        // Căn cửa sổ giữa màn hình
        if (width > WINDOW_SIZE.width && height > WINDOW_SIZE.height){
            setLocation(new Point((width / 2) - (WINDOW_SIZE.width / 2), (height / 2) - (WINDOW_SIZE.height / 2)));
        }
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        main_scroll_pane.getVerticalScrollBar().setUnitIncrement(16);

        MainWindow mw = this;
        filters = new ReferencesList<>();
        filters.pushBack(new StoreFilter());

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

        fetch_dvd_list();
        b_refresh.addActionListener(e -> fetch_dvd_list());
    }
}

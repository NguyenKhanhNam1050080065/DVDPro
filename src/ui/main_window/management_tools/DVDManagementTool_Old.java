package ui.main_window.management_tools;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.*;
import entry.WindowNavigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class DVDManagementTool_Old extends JFrame {
    private JPanel MainPanel;
    private JButton b_return;
    private JButton b_confirm;
    private JLabel dvd_name;
    private JButton b_undo;
    private JButton b_confiscate;
    private JList<String> l_renting;
    private JList<String> l_confiscating;
    private JButton b_refresh;
    private JScrollPane scroll_1;
    private JScrollPane scroll_2;
    private JButton b_confiscate_all;
    private JButton b_undo_all;

    public static final Dimension WINDOW_SIZE  = new Dimension(800, 600);
    private final SafeFlag returnLock = new SafeFlag(false);
    private final SafeFlag modified = new SafeFlag(false);
    private final Object listLock = new Object();
    private final DefaultListModel<String> m_renting;
    private final DefaultListModel<String> m_confiscating;
    private final java.util.List<String> uid_renting = new ArrayList<>();
    private final java.util.List<String> uid_confiscating = new ArrayList<>();

    static class ConfiscatingAgent {
        private final DVDManagementTool_Old mt;
        private final String uid;
        private final String imdb_link;
        private final SafeFlag flag1;
        private final SafeFlag flag2;

        public ConfiscatingAgent(DVDManagementTool_Old mt, String uid, String imdb_link, SafeFlag flag1, SafeFlag flag2){
            this.mt = mt;
            this.uid = uid;
            this.imdb_link = imdb_link;
            this.flag1 = flag1;
            this.flag2 = flag2;
            start();
        }
        private void start(){
            Map<String, Object> clear_update = new HashMap<>();
            clear_update.put("renting", "");
            FirestoreQuery clear_query = new FirestoreQuery(FirestoreQuery.QueryType.PATCH_DOCUMENT)
                    .onCollection("users").onDocument(uid)
                    .update(clear_update).documentExisted(true).updateMask("renting");
            WindowNavigator.db().query(clear_query, new FirestoreTaskReceiver() {
                @Override
                public void connectionFailed() {
                    flag1.set();
                }

                @Override
                public void requestFailed(int i, Map<String, String> map, String s) {
                    flag1.set();
                }

                @Override
                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                    flag1.set();
                }
            });
            // Unused
            flag2.set();
        }
    }
    static class ConfirmConfiscationDispatcher {
        private final DVDManagementTool_Old mt;
        private final java.util.List<String> uid;
        private final String target_name;
        private final SafeFlag finishFlag = new SafeFlag(false);

        public ConfirmConfiscationDispatcher(DVDManagementTool_Old mt, java.util.List<String> uid, String target_name){
            this.mt = mt;
            this.uid = uid;
            this.target_name = target_name;
            start();
        }
        private void start(){
            final SafeFlag[] flags = new SafeFlag[uid.size() * 2];
            for (int i = 0; i < flags.length; i++){
                flags[i] = new SafeFlag(false);
            }
            for (int i = 0; i < uid.size(); i++){
                new ConfiscatingAgent(mt, uid.get(i), target_name, flags[i * 2], flags[(i * 2) + 1]);
            }
            for (SafeFlag f : flags){
                while (!f.get()) { continue; }
            }
            finishFlag.set();
        }
        public void sync() {
            while (!finishFlag.get()) { continue; }
        }
    }

    private void setList(Map<String, FirestoreDocument> initial_list) {
        if (initial_list == null) return;
        synchronized (listLock){
            m_renting.clear();
            m_confiscating.clear();
            uid_renting.clear();
            uid_confiscating.clear();
            for (Map.Entry<String, FirestoreDocument> E : initial_list.entrySet()){
                String username = E.getValue().getFields().get("username").toString();
                String uid = E.getValue().getFields().get("uid").toString();
                m_renting.addElement(username);
                uid_renting.add(uid);
            }
        }
    }
    private void moveItem(int idx, DefaultListModel<String> from, java.util.List<String> uid_from, DefaultListModel<String> to, java.util.List<String> uid_to){
        if (idx == -1) return;
        String name = from.elementAt(idx);
        String uid = uid_from.get(idx);
        from.remove(idx);
        uid_from.remove(idx);
        to.addElement(name);
        uid_to.add(uid);
    }
    private void confirmMovement(String imdb_link){
        lockInput();
        new ConfirmConfiscationDispatcher(this, uid_confiscating, imdb_link).sync();
//        unlockInput();
        refreshLists(imdb_link);
    }
    public void changeInput(boolean state){
        b_return.setEnabled(state);
        b_confirm.setEnabled(state);
        b_refresh.setEnabled(state);
        b_confiscate_all.setEnabled(state);
        b_undo_all.setEnabled(state);
        b_confiscate.setEnabled(state);
        b_undo.setEnabled(state);
    }
    public void lockInput(){
        changeInput(false);
    }
    public void unlockInput(){
        changeInput(true);
    }
    public void refreshLists(String target_link) {
        DVDManagementTool_Old mt = this;
        final SafeFlag finished = new SafeFlag(false);
        lockInput();
        FirestoreQuery fetch_all_renting = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                .from("users").where("renting", FirestoreQuery.Operator.EQUAL, target_link);
        WindowNavigator.db().query(fetch_all_renting, new FirestoreTaskReceiver() {
            @Override
            public void connectionFailed() {
                JOptionPane.showMessageDialog(mt, "Không thể làm mới: Không kết nối được đến máy chủ Firebase");
                finished.set();
            }
            @Override
            public void requestFailed(int i, Map<String, String> map, String s) {
                JOptionPane.showMessageDialog(mt, "Không thể làm mới: Yêu cầu thất bại");
                finished.set();
            }

            @Override
            public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                setList(firestoreTaskResult.getDocuments());
                finished.set();
            }
        });
        while (!finished.get()) { continue; }
        unlockInput();
    }

    public DVDManagementTool_Old(String target_name, String target_link, Map<String, FirestoreDocument> initial_list){
        super("Quản lý DVD");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        m_renting = (DefaultListModel<String>) l_renting.getModel();
        m_confiscating = (DefaultListModel<String>) l_confiscating.getModel();

        scroll_1.getVerticalScrollBar().setUnitIncrement(16);
        scroll_2.getVerticalScrollBar().setUnitIncrement(16);
        dvd_name.setText(target_name);
        setList(initial_list);
        DVDManagementTool_Old mt = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!returnLock.get()) returnLock.set();
                mt.dispose();
            }
        });
        b_return.addActionListener(e -> {
            returnLock.set();
            mt.dispose();
        });
        b_confirm.addActionListener(e -> {
            modified.set();
            confirmMovement(target_link);
        });
        b_refresh.addActionListener(e -> {
            refreshLists(target_link);
        });
        b_confiscate.addActionListener(e -> {
            synchronized (listLock){
                int idx = l_renting.getSelectedIndex();
                moveItem(idx, m_renting, uid_renting, m_confiscating, uid_confiscating);
            }
        });
        b_undo.addActionListener(e -> {
            synchronized (listLock){
                int idx = l_confiscating.getSelectedIndex();
                moveItem(idx, m_confiscating, uid_confiscating, m_renting, uid_renting);
            }
        });
        b_confiscate_all.addActionListener(e -> {
            synchronized (listLock){
                while (!uid_renting.isEmpty()){
                    moveItem(0, m_renting, uid_renting, m_confiscating, uid_confiscating);
                }
            }
        });
        b_undo_all.addActionListener(e -> {
            synchronized (listLock){
                while (!uid_confiscating.isEmpty()){
                    moveItem(0, m_confiscating, uid_confiscating, m_renting, uid_renting);
                }
            }
        });
    }
    public void sync(){
//        while (!returnLock.get()) { continue; }
        returnLock.waitToFinish();
    }
    public boolean isModified(){
        return modified.get();
    }
    private void createUIComponents() {
        // TODO: place custom component creation code here
        l_renting = new JList<>(new DefaultListModel<>());
        l_confiscating = new JList<>(new DefaultListModel<>());
    }
}

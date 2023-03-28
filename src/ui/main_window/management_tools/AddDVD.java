package ui.main_window.management_tools;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.FirestoreModelException;
import com.cycastic.javabase.firestore.FirestoreQuery;
import com.cycastic.javabase.firestore.NoConnectionException;
import com.cycastic.javabase.firestore.QueryRefusedException;
import entry.WindowNavigator;
import misc.L;
import models.DVDCollection;
import models.DVDModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AddDVD extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(700, 500);
    private JPanel MainPanel;
    private JButton b_return;
    private JButton b_accept;
    private JTextField f_capsule;
    private JTextField f_name;
    private JTextField f_category;
    private JTextField f_director;
    private JTextField f_duration;
    private JTextField f_imdb;
    private JTextField f_age;
    private JTextField f_year;
    private final SafeFlag returnLock = new SafeFlag(false);
    private final boolean patch;
    private DVDModel original;
    private void finisher(){
        returnLock.set();
        dispose();
    }
    public void changeInputState(boolean state){
        f_name.setEnabled(state && !patch);
        f_capsule.setEnabled(state);
        f_category.setEnabled(state);
        f_director.setEnabled(state);
        f_duration.setEnabled(state);
        f_imdb.setEnabled(state && !patch);
        f_age.setEnabled(state);
        f_year.setEnabled(state);
        b_accept.setEnabled(state);
        b_return.setEnabled(state);
    }
    public void lockInput(){
        changeInputState(false);
    }
    public void unlockInput(){
        changeInputState(true);
    }
    private void submit(){
        new Thread(() -> {
            synchronized (returnLock){
                final String name = f_name.getText();
                final String capsule = f_capsule.getText();
                final String categoryRaw = f_category.getText();
                final String director = f_director.getText();
                final String durationRaw = f_duration.getText();
                final String imdb = f_imdb.getText();
                final String ageRatingRaw = f_age.getText();
                final String yearRaw = f_year.getText();

                if (name.isEmpty() || capsule.isEmpty() || categoryRaw.isEmpty() ||
                    director.isEmpty() || durationRaw.isEmpty() || imdb.isEmpty() ||
                    ageRatingRaw.isEmpty() || yearRaw.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Thông tin chưa đầy dủ");
                    return;
                }

                final long duration;
                final long year;
                try {
                    duration = Long.parseLong(durationRaw);
                    year = Long.parseLong(yearRaw);
                    if (duration < 10 || year < 1950) throw new NumberFormatException();
                } catch (NumberFormatException ex){
                    JOptionPane.showMessageDialog(this, "Đầu vào không hợp lệ");
                    return;
                }
//                Pattern capsulePattern = Pattern.compile("/(?:https?:\\/\\/.*\\.(?:png|jpg|jpeg|gif))/");
//                Pattern imdbPattern = Pattern.compile("/(?:https?:\\/\\/)?(?:www\\.)?(?:imdb\\.com)\\/title\\/.*");
//                if (!capsulePattern.matcher(capsule).find()){
//                    JOptionPane.showMessageDialog(this, "Capsule không hợp lệ");
//                    return;
//                } else if (!imdbPattern.matcher(imdb).find()){
//                    JOptionPane.showMessageDialog(this, "IMDB link không hợp lệ");
//                    return;
//                }
                lockInput();
                FirestoreQuery existenceCheck = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                        .from("dvds_info").where("imdb_link", FirestoreQuery.Operator.EQUAL, imdb);
                DVDCollection existenceCheckCol = new DVDCollection(WindowNavigator.db(), true, existenceCheck);
                if (!patch){
                    try {
                        Map<String, DVDModel> dvds = existenceCheckCol.getDocuments();
                        if (dvds.size() > 0){
                            JOptionPane.showMessageDialog(this, "DVD đã tồn tại");
                            unlockInput();
                            return;
                        }
                    } catch (FirestoreModelException ex){
                        JOptionPane.showMessageDialog(this, "Không thể kiểm tra sự tồn tại của DVD trong Firestore");
                        unlockInput();
                        return;
                    }
                }


                String[] categorySplit = categoryRaw.split(";");
                List<String> category = new ArrayList<>();
                for (String s : categorySplit){
                    category.add(s.trim());
                }
                String ratedType;
                long ratedAge;
                String[] ratingSplit = ageRatingRaw.split("-");
                ratedType = ratingSplit[0].trim();
                if (ratingSplit.length > 1) {
                    try {
                        ratedAge = Long.parseLong(ratingSplit[1].trim());
                        if (ratedAge < 5) throw new NumberFormatException();
                    } catch (NumberFormatException ex){
                        ratedAge = 18;
                    }
                } else ratedAge = 18;

                DVDModel dvd = new DVDModel(WindowNavigator.db(), true);
                dvd.setCapsuleArt(capsule);
                dvd.setCategory(category);
                dvd.setDirector(director);
                dvd.setDuration(duration);
                dvd.setImdbLink(imdb);
                dvd.setName(name);
                dvd.setRatedType(ratedType);
                dvd.setRatedAge(ratedAge);
                dvd.setStock(0L);
                dvd.setYear(year);

                final FirestoreQuery updateQuery;
                if (patch){
                    updateQuery = new FirestoreQuery(FirestoreQuery.QueryType.PATCH_DOCUMENT)
                            .onCollection("dvds_info").onDocument(original.getDocumentName()).documentExisted(true)
                            .updateMask("capsule_art").updateMask("category").updateMask("director").updateMask("duration")
                            .updateMask("rated_age").updateMask("rated_type").updateMask("year");
                } else  {
                    updateQuery = new FirestoreQuery(FirestoreQuery.QueryType.CREATE_DOCUMENT)
                            .onCollection("dvds_info");
                }
                DVDCollection col = new DVDCollection(WindowNavigator.db(), true, updateQuery);
                col.serializeSingle(dvd);
                try {
                    col.evaluate();
                    JOptionPane.showMessageDialog(this, "Thành công");
                } catch (FirestoreModelException ex){
                    if (ex.getClass() == NoConnectionException.class){
                        JOptionPane.showMessageDialog(this, "Kết nối thất bại: Không có Internet");
                    } else if (ex.getClass() == QueryRefusedException.class) {
                        JOptionPane.showMessageDialog(this, "Kết nối thất bại: Yêu cầu bị từ chối");
                    }
                    L.log("AddDVD", ex.toString());
                }
                returnLock.set();
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }
        }).start();
    }
    private void displayInfo(){
        if (!patch) return;
        StringBuilder category = new StringBuilder();
        final int categorySize = original.getCategory().size();
        for (int i = 0; i < categorySize - 1; i++){
            category.append(original.getCategory().get(i)).append("; ");
        }
        if (categorySize > 0) category.append(original.getCategory().get(categorySize - 1));

        f_name.setText(original.getName());
        f_capsule.setText(original.getCapsuleArt());
        f_category.setText(category.toString());
        f_director.setText(original.getDirector());
        f_duration.setText(String.valueOf(original.getDuration()));
        f_imdb.setText(original.getImdbLink());
        f_age.setText("%s-%d".formatted(original.getRatedType(), original.getRatedAge()));
        f_year.setText(String.valueOf(original.getYear()));
    }
    public AddDVD(boolean isPatching, DVDModel model){
        super("Thêm DVD");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);

        patch = isPatching;
        original = model;
        displayInfo();
        unlockInput();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finisher();
            }
        });
        b_return.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        b_accept.addActionListener(e -> submit());
    }
    public void run(){
        returnLock.waitToFinish();
    }
}

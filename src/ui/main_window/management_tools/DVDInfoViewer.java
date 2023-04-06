package ui.main_window.management_tools;

import com.cycastic.javabase.dispatcher.AsyncEngine;
import com.cycastic.javabase.dispatcher.SafeFlag;
import entry.WindowNavigator;
import models.DVDModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DVDInfoViewer extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(600, 400);
    public static final Dimension CAPSULE_SIZE = new Dimension(131, 191);
    private JPanel MainPanel;
    private JLabel capsule_label;
    private JLabel f_name;
    private JButton b_return;
    private JButton b_input;
    private JLabel f_director;
    private JLabel f_duration;
    private JLabel f_age;
    private JLabel f_stock;
    private JLabel f_year;
    private JLabel f_category;
    private JButton b_output;
    private JButton b_update;

    private final SafeFlag returnLock = new SafeFlag(false);
    private final SafeFlag modified = new SafeFlag(false);
    private final Object submitLock = new Object();
    private final AsyncEngine engine = new AsyncEngine(AsyncEngine.MODE_HOT);
    private final boolean isAdmin;
    private boolean result = false;
    private void finisher(){
        engine.terminate();
        returnLock.set();
        dispose();
    }
    public void changeInputState(boolean state){
        b_input.setEnabled(state);
        b_output.setEnabled(state);
        b_return.setEnabled(state);
        b_update.setEnabled(state && isAdmin);
    }
    public void lockInput(){
        changeInputState(false);
    }
    public void unlockInput(){
        changeInputState(true);
    }

    public DVDInfoViewer(DVDModel details, Image capsule_art, boolean is_admin){
        super("Thuê DVD");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setMinimumSize(WINDOW_SIZE);
        final DVDInfoViewer rc = this;

        this.isAdmin = is_admin;
        b_update.setEnabled(is_admin);
        final String duration_txt = "%dh %dm"; long _f, _h, _m;
        final String rating_text  = "%s-%d"; long rated_age = details.getRatedAge();
        _f = details.getDuration();
        _h = _f / 60;
        _m = _f - (60 * _h);
        StringBuilder category_str = new StringBuilder();
        java.util.List<String> _category = details.getCategory();
        for (int i = 0, s = _category.size(); i < s && i < 2; i++){
            if (i > 0) category_str.append(" • ");
            category_str.append(_category.get(i));
        }
        String imdb_link = details.getImdbLink();
        String dvd_name = details.getName();
        final ImageIcon capsule_icon;
        if (capsule_art != null) capsule_icon = new ImageIcon(capsule_art.getScaledInstance(CAPSULE_SIZE.width, CAPSULE_SIZE.height, Image.SCALE_SMOOTH));
        else capsule_icon = null;

//        if (already_rented) {
//            b_input.setEnabled(false);
//            b_input.setToolTipText("Bạn đã thuê DVD này");
//        }
        f_name.setText(dvd_name);
        f_director.setText(details.getDirector());
        f_duration.setText(duration_txt.formatted(_h, _m));
        f_age.setText(rating_text.formatted(details.getRatedType().toString(), rated_age));
        f_stock.setText(String.valueOf(details.getStock()));
        f_year.setText(String.valueOf(details.getYear()));
        f_category.setText(category_str.toString());
        engine.dispatch(()-> capsule_label.setIcon(capsule_icon)).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finisher();
            }
        });
        b_return.addActionListener(e -> {
            result = false;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });
        b_update.addActionListener(e -> engine.dispatch(() -> {
            lockInput();
            new AddDVD(true, details).run();
            unlockInput();
        }).start());
        b_input.addActionListener(e -> engine.dispatch(() -> {
            lockInput();
            new NewOperation(details.getImdbLink(), details.getName(), details.getDocumentName(), details.getStock(), true).run();
            unlockInput();
        }).start());
        b_output.addActionListener(e -> engine.dispatch(() -> {
            lockInput();
            new NewOperation(details.getImdbLink(), details.getName(), details.getDocumentName(), details.getStock(), false).run();
            unlockInput();
        }).start());
    }
    public boolean isModified(){
        return modified.get();
    }
    public boolean run(){
        returnLock.waitToFinish();
        return result;
    }

}

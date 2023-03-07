package ui.auth_window.user_create;

import com.cycastic.javabase.dispatcher.SafeFlag;
import entry.WindowNavigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class UserCreateForm extends JFrame {
    private JPanel MainPanel;
    private JTextField f_username;
    private JButton b_request;
    private JComboBox<Integer> f_day;
    private JComboBox<Integer> f_month;
    private JComboBox<Integer> f_year;

    public static final Dimension WINDOW_SIZE  = new Dimension(450, 270);
    public static final int STARTING_YEAR = 1980;

    private final SafeFlag returnLock = new SafeFlag(false);
    private final Object spinnerLock = new Object();
    private final Object submitLock = new Object();
    private final int[] currentDate;
    private UserInfo info;
    private boolean editing = false;

    public static class UserInfo {
        public final String username;
        public final long unixBirthday;

        public UserInfo(String u, long b){
            username = u; unixBirthday = b;
        }
    }
    public static long dateToUnix(int day, int month, int year){
        return new GregorianCalendar(year, month - 1, day).getTime().getTime() / 1000L;
    }
    public static Date unixToDate(long unixTime){
        return new Date(unixTime * 1000);
    }
    private void addStuff(int month, int year){
        editing = true;
        f_day.removeAllItems();
        f_month.removeAllItems();
        f_year.removeAllItems();
        if (month < 1 || month > 12 || year < STARTING_YEAR) {
            editing = false;
            return;
        }
        int limit = 31;
        if (month == 4 || month == 6 || month == 9 || month == 11) limit = 30;
        else if (month == 2) limit = year % 4 == 0 ? 29 : 28;
        for (int i = 1; i <= limit; i++){
            f_day.addItem(i);;
        }
        for (int i = 1; i <= 12; i++){
            f_month.addItem(i);
            if (i == month) f_month.setSelectedItem(i);
        }
        for (int i = STARTING_YEAR; i <= currentDate[2] - 1; i++){
            f_year.addItem(i);
            if (i == year) f_year.setSelectedItem(i);
        }
        editing = false;
    }
    public static Calendar getToday(){
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }
    public static int getAgeByDob(Date dob){
        int year = UserCreateForm.getToday().get(Calendar.YEAR);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(dob);
        return year - calendar.get(Calendar.YEAR);
    }
    public UserCreateForm(){
        super("Thông tin người dùng");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setResizable(false);

        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        currentDate = new int[3];
        currentDate[0] = calendar.get(Calendar.DAY_OF_MONTH);
        currentDate[1] = calendar.get(Calendar.MONTH) + 1;
        currentDate[2] = calendar.get(Calendar.YEAR);

        Window mw = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                synchronized (submitLock){
                    if (!returnLock.get()) {
                        info = new UserInfo("Mary Sue", 946684805);
                        returnLock.set();
                    }
                }
                mw.dispose();
            }
        });
        b_request.addActionListener(e -> {
            synchronized (submitLock){
                if (returnLock.get()) return;
                info = new UserInfo(f_username.getText(), dateToUnix(f_day.getSelectedIndex() + 1,
                        f_month.getSelectedIndex() + 1, f_year.getSelectedIndex() + STARTING_YEAR));
                returnLock.set();
                mw.dispose();
            }
        });
        f_month.addActionListener(e -> {
            synchronized (spinnerLock){
                if (editing) return;
                addStuff(f_month.getSelectedIndex() + 1, f_year.getSelectedIndex() + STARTING_YEAR);
            }
        });
        f_year.addActionListener(e -> {
            synchronized (spinnerLock){
                if (editing) return;
                addStuff(f_month.getSelectedIndex() + 1, f_year.getSelectedIndex() + STARTING_YEAR);
            }
        });

        addStuff(1, 2000);
    }
    public UserInfo getUserInfo(){
        while (!returnLock.get()) { continue; }
        return info;
    }

}

package ui.auth_window;

import entry.WindowNavigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AuthWindow extends JFrame {
    public static final Dimension WINDOW_SIZE = new Dimension(450, 270);

    private JPanel MainPanel;
    private JLabel l_main;
    private JTextField f_username;
    private JLabel l_password;
    private JLabel l_username;
    private JSeparator sep;
    private JButton b_change;
    private JButton b_request;
    private JLabel l_password2;
    private JPasswordField f_password;
    private JPasswordField f_password2;

    private AuthWindowAdapter adapter = null;
    // 0: Login
    // 1: Register
    private boolean is_logging_in = true;

    private void set_mode(boolean mode){
        is_logging_in = mode;
        f_password2.setEnabled(!is_logging_in);
        l_password2.setEnabled(!is_logging_in);
        if (is_logging_in) {
            l_main.setText("Đăng nhập");
            b_change.setText("Chuyển qua đăng ký");
            b_request.setText("Đăng nhập");
        } else {
            l_main.setText("Đăng ký");
            b_change.setText("Chuyển qua đăng nhập");
            b_request.setText("Đăng ký");
        }
    }
    private void change_mode(){
        set_mode(!is_logging_in);
    }

    private void request_action(){
        synchronized (this){
            String username = f_username.getText();
            String password = String.valueOf(f_password.getPassword());
            String password2 = String.valueOf(f_password2.getPassword());
            f_password.setText("");
            f_password2.setText("");
            if (adapter == null) return;
            if (is_logging_in) adapter.user_login(username, password);
            else adapter.user_signup(username, password, password2);
            // Tiêu hủy mật khẩu
            Runtime.getRuntime().gc();
        }
    }

    public AuthWindow(){
        super("Quản lý đĩa DVD UwU");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setResizable(false);
        //Set mode ở trong constructor để thuận tiện cho việc thử nghiệm
        set_mode(is_logging_in);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                synchronized (this) {
                    if (adapter != null) adapter.window_closing(e.getWindow());
                }
            }
        });
        b_change.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (this){
                    f_password2.setText("");
                    change_mode();
                }
            }
        });
        b_request.addActionListener(e -> request_action());
        f_username.addActionListener(e -> request_action());
        f_password.addActionListener(e -> request_action());
        f_password2.addActionListener(e -> request_action());
    }
    public void lock_input(){
        b_request.setEnabled(false);
        b_change.setEnabled(false);
    }
    public void unlock_input(){
        b_request.setEnabled(true);
        b_change.setEnabled(true);
    }
    public void set_adapter(AuthWindowAdapter a){
        synchronized (this) {
            adapter = a;
        }
    }

}

package ui.auth_window;

import java.awt.*;

public abstract class AuthWindowAdapter {

    public abstract void window_closing(Window win);
    public abstract void user_login(String username, String password);
    public abstract void user_signup(String username, String password, String password2);
}

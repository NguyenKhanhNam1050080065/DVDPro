package entry;

import com.cycastic.javabase.auth.FirebaseAuthListener;
import com.cycastic.javabase.auth.FirebaseAuthToken;
import com.cycastic.javabase.auth.FirebaseAuthTokenWrapper;
import com.cycastic.javabase.firestore.FirestoreQuery;
import com.cycastic.javabase.firestore.FirestoreTaskReceiver;
import com.cycastic.javabase.firestore.FirestoreTaskResult;
import misc.L;
import org.json.JSONObject;
import ui.auth_window.AuthWindow;
import ui.auth_window.AuthWindowAdapter;
import ui.auth_window.user_create.UserCreateForm;

import javax.swing.*;
import java.awt.*;
import java.util.*;


public class Main {
    private static class AuthChangedHandler {
        private static void userspaceAllotted(){
            WindowNavigator.createMainWindow();
        }
        public static void connectionFailed(AuthWindow aw){
            JOptionPane.showMessageDialog(aw, "Không thể kết nối đến máy chủ Firebase. Hãy kiểm tra lại tín hiệu mạng.");

            aw.unlock_input();
        }
        public static void requestFailed(AuthWindow aw, String s){
            if (s.isEmpty()){
                JOptionPane.showMessageDialog(aw, "Xác thực thất bại.");
                aw.unlock_input();
                return;
            }
            JSONObject err_parse = new JSONObject(s);
            Map<String, Object> err_map = (Map<String, Object>) err_parse.toMap().get("error");
            String err_code = err_map.get("message").toString();
            JOptionPane.showMessageDialog(aw, "Xác thực thất bại. Mã lỗi: %s".formatted(err_code));

            aw.unlock_input();
        }
        public static void authChanged(AuthWindow aw){
            JOptionPane.showMessageDialog(aw, "Xác thực thành công");
            FirebaseAuthTokenWrapper authWrapper = WindowNavigator.auth().getAuthWrapper();
            String uid = authWrapper.getAuthToken().getLocalId();
            WindowNavigator.db().enrollToken(authWrapper);
            FirestoreQuery checkExistence = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                    .from("users").where("uid", FirestoreQuery.Operator.EQUAL, uid);
            WindowNavigator.db().query(checkExistence, new FirestoreTaskReceiver() {
                @Override
                public void connectionFailed() {
                    JOptionPane.showMessageDialog(aw, "Không thể kết nối đến máy chủ Firestore, hãy kiểm tra lại kết nối Internet");
                    aw.unlock_input();
                }
                @Override
                public void requestFailed(int i, Map<String, String> map, String s) {
                    JOptionPane.showMessageDialog(aw, "Yêu cầu thất bại. Mã lỗi: %d".formatted(i));
                    aw.unlock_input();
                }
                @Override
                public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                    if (firestoreTaskResult.getDocuments().isEmpty()){
                        UserCreateForm.UserInfo info = new UserCreateForm().getUserInfo();
                        FirestoreQuery allocateUserProfile = new FirestoreQuery(FirestoreQuery.QueryType.CREATE_DOCUMENT);
                        Map<String, Object> document = new HashMap<>();
                        document.put("dob", info.unixBirthday);
                        document.put("username", info.username);
                        // Homelander
                        document.put("pfp", "https://i.pinimg.com/originals/9a/bb/94/9abb9492b3743a8d65b3052b969a9221.jpg");
                        document.put("renting", "");
                        document.put("lost", new ArrayList<>());
                        document.put("uid", uid);
                        document.put("rented", 0);
                        allocateUserProfile.onCollection("users").create(document).onDocument(uid);
                        WindowNavigator.db().query(allocateUserProfile, new FirestoreTaskReceiver() {
                            @Override
                            public void connectionFailed() {
                                JOptionPane.showMessageDialog(aw, "Không thể kết nối đến máy chủ Firestore, hãy kiểm tra lại kết nối Internet");
                                aw.unlock_input();
                            }
                            @Override
                            public void requestFailed(int i, Map<String, String> map, String s) {
                                JOptionPane.showMessageDialog(aw, "Yêu cầu thất bại. Mã lỗi: %d".formatted(i));
                                aw.unlock_input();
                            }
                            @Override
                            public void queryCompleted(FirestoreTaskResult firestoreTaskResult) {
                                L.log("Main", "Userspace allotted");
                                userspaceAllotted();
                            }
                        });
                    } else userspaceAllotted();
                }
            });
        }
    }

    public static void main(String[] args) {
        WindowNavigator.setup();
        AuthWindow aw = WindowNavigator.createAuthWindow();
        if (aw == null) return;
        aw.set_adapter(new AuthWindowAdapter() {
            @Override
            public void window_closing(Window win) {
                WindowNavigator.closeWindow(win);
            }

            @Override
            public void user_login(String username, String password) {
                aw.lock_input();
                WindowNavigator.auth().loginWithEmailAndPassword(username, password, new FirebaseAuthListener() {
                    @Override
                    public void onConnectionFailed() {
                        AuthChangedHandler.connectionFailed(aw);
                    }

                    @Override
                    public void onRequestFailed(int i, Map<String, String> map, String s) {
                        AuthChangedHandler.requestFailed(aw, s);
                    }

                    @Override
                    public void onAuthChanged(FirebaseAuthToken firebaseAuthToken) {
                        AuthChangedHandler.authChanged(aw);
                    }
                });
            }

            @Override
            public void user_signup(String username, String password, String password2) {
                if (!Objects.equals(password, password2)) {
                    JOptionPane.showMessageDialog(aw, "Nhập lại mật khẩu sai");
                    return;
                }
                aw.lock_input();
                WindowNavigator.auth().signupWithEmailAndPassword(username, password, new FirebaseAuthListener() {
                    @Override
                    public void onConnectionFailed() {
                        AuthChangedHandler.connectionFailed(aw);
                    }

                    @Override
                    public void onRequestFailed(int i, Map<String, String> map, String s) {
                        AuthChangedHandler.requestFailed(aw, s);
                    }

                    @Override
                    public void onAuthChanged(FirebaseAuthToken firebaseAuthToken) {
                        AuthChangedHandler.authChanged(aw);
                    }
                });
            }
        });
    }
}

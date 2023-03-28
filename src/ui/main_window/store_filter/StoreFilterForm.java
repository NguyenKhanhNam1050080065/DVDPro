package ui.main_window.store_filter;

import com.cycastic.javabase.dispatcher.SafeFlag;
import com.cycastic.javabase.firestore.FirestoreQuery;
import entry.WindowNavigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class StoreFilterForm extends JFrame {
    public static final Dimension WINDOW_SIZE  = new Dimension(500, 250);
    private static final String[] FILTER_FIELDS = { "Category", "Duration", "Rated Age", "Rated type", "Year" };
    private static final String[] DB_FIELDS     = { "category", "duration", "rated_age", "rated_type", "year" };
    private static final int[] FILTER_BITMASKS =  { 1,          2,         4,           8,            16 };
    private static final FirestoreQuery.Operator[] ALLOWED_OPS = { FirestoreQuery.Operator.GREATER_THAN,
            FirestoreQuery.Operator.ARRAY_CONTAINS_ANY, FirestoreQuery.Operator.GREATER_THAN_OR_EQUAL,
            FirestoreQuery.Operator.LESS_THAN, FirestoreQuery.Operator.LESS_THAN_OR_EQUAL,
            FirestoreQuery.Operator.EQUAL };
    // 2 + 4 + 16 = 22
    // Firestore ko hỗ trợ toán tử "in" :<
    private static final int[] SELECTABLE_OPS_MASKS = { 22, 0, 22, 22, 22, 8 + 1 };

    private static final Object[] ALLOWED_VALUES = { "M", "TV", "PG", 13, 17, 18, 30, 60, 120, 2015, 2018, 2022,
            "Action", "Crime", "Fantasy", "Drama", "Adventure", "Comedy", "Military", "Thriller" };

    private static final int[] SELECTABLE_VALUES_MASKS = { 8, 8, 8, 4, 4, 4, 2, 2, 2, 16, 16, 16,
            1, 1, 1, 1, 1, 1, 1, 1};
    private JComboBox<String> filterField;
    private JComboBox<String> filterValue;
    private JTextField filterIndex;
    private JComboBox<String> filterOperation;
    private JPanel MainPanel;
    private JButton submitButton;
    private final SafeFlag returnLock = new SafeFlag(false);
    private final Object submitLock = new Object();
    private final Object spinnerLock = new Object();
    private StoreFilterSettings ret = null;
    private boolean editing = false;

    public static class StoreFilterSettings {
        public final String field;
        public final FirestoreQuery.Operator operator;
        public final Object value;
        public StoreFilterSettings(String f, FirestoreQuery.Operator op, Object val){
            field = f; operator = op; value = val;
        }
        public StoreFilterSettings(){
            this("stock", FirestoreQuery.Operator.GREATER_THAN, -1);
        }
    }
    private void editSpinner(int fieldIndex){
        if (fieldIndex == -1)return;
        editing = true;
//        filterField.removeAllItems();
        filterOperation.removeAllItems();
        filterValue.removeAllItems();
        int currentBitmask = FILTER_BITMASKS[fieldIndex];
        for (int i = 0; i < ALLOWED_OPS.length; i++){
            FirestoreQuery.Operator op = ALLOWED_OPS[i];
            int opMask = SELECTABLE_OPS_MASKS[i];
            if ((opMask & currentBitmask) != 0) filterOperation.addItem(FirestoreQuery.Operator.toString(op));
        }
        for (int i = 0; i < ALLOWED_VALUES.length; i++){
            Object value = ALLOWED_VALUES[i];
            int valMask = SELECTABLE_VALUES_MASKS[i];
            if ((valMask & currentBitmask) != 0) filterValue.addItem(value.toString());
        }
        editing = false;
    }
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }
    private StoreFilterSettings fetchInternal(){
        String _field = DB_FIELDS[filterField.getSelectedIndex()];
        FirestoreQuery.Operator _op = FirestoreQuery.Operator.fromString(filterOperation.getSelectedItem().toString());
        String _valueStr = filterValue.getSelectedItem().toString();
        Object _value;
        if (isInteger(_valueStr)) _value = Integer.parseInt(_valueStr);
        else _value = _valueStr;
        return new StoreFilterSettings(_field, _op, _value);
    }

    public StoreFilterForm(int index){
        super("Thêm phễu lọc UmU");
        setLocation(WindowNavigator.getCenterOfScreen(this, WINDOW_SIZE));
        setContentPane(MainPanel);
        setVisible(true);
        setSize(WINDOW_SIZE);
        setResizable(false);
        filterIndex.setText(String.valueOf(index));

        StoreFilterForm sf = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            synchronized (submitLock){
                if (!returnLock.get()) {
                    ret = new StoreFilterSettings();
                    returnLock.set();
                }
            }
            sf.dispose();
            }
        });
        filterField.addActionListener(e -> {
            synchronized (spinnerLock){
                if (editing) return;
                editSpinner((filterField.getSelectedIndex()));
            }
        });
        submitButton.addActionListener(e -> {
            synchronized (submitLock){
                if (returnLock.get()) return;
                ret = fetchInternal();
                returnLock.set();
                sf.dispose();
            }
        });
        editing = true;
        for (String f : FILTER_FIELDS) {
            filterField.addItem(f);
        }
        editing = false;
        filterField.setSelectedIndex(0);
    }
    public StoreFilterSettings getFilterSettings(){
        while (!returnLock.get()) { continue; }
        return ret;
    }
}

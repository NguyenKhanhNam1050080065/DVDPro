package models;

import com.cycastic.javabase.firestore.Firestore;
import com.cycastic.javabase.firestore.FirestoreModel;

public class LedgerModel extends FirestoreModel {
    private Long amount = 0L;
    private String dvd = "";
    private Boolean isInput = false;
    private Long time = 0L;
    private String vendor = "";
    private String commissioner = "<anon>";
    public LedgerModel(Firestore host, boolean lazyEvaluation) {
        super(host, lazyEvaluation);
    }
    public Boolean getInput() {
        return isInput;
    }
    public long getAmount() {
        return amount;
    }
    public long getTime() {
        return time;
    }
    public String getDvd() {
        return dvd;
    }
    public String getVendor() {
        return vendor;
    }

    public String getCommissioner() {
        return commissioner;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
    public void setDvd(String dvd) {
        this.dvd = dvd;
    }
    public void setInput(Boolean input) {
        isInput = input;
    }
    public void setTime(Long time) {
        this.time = time;
    }
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setCommissioner(String commissioner) {
        this.commissioner = commissioner;
    }
}

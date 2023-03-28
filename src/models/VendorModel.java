package models;

import com.cycastic.javabase.firestore.Firestore;
import com.cycastic.javabase.firestore.FirestoreModel;

public class VendorModel extends FirestoreModel {
    private String email = "";
    private String guid = "";
    private String name = "";

    public VendorModel duplicate(){
        return new VendorModel(this);
    }
    public VendorModel(Firestore host, boolean lazyEvaluation) {
        super(host, lazyEvaluation);
    }
    public VendorModel(VendorModel other){
        super(other.host, other.lazyEvaluation);
        email = other.email;
        guid = other.guid;
        name = other.name;
        documentName = other.getDocumentName();
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getGuid() {
        return guid;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setGuid(String guid) {
        this.guid = guid;
    }
}

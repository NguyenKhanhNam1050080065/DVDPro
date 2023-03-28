package models;

import com.cycastic.javabase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class VendorCollection extends FirestoreCollectionModel<VendorModel> {
    private Map<String, Object> lastSerialization = new HashMap<>();
    public VendorCollection setRead(){
        baseQuery = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                .from("vendors")/*.where("stock", FirestoreQuery.Operator.GREATER_THAN, 0)*/;
        return this;
    }
    public VendorCollection setWrite(boolean isPatching){
        baseQuery = new FirestoreQuery(isPatching ? FirestoreQuery.QueryType.PATCH_DOCUMENT : FirestoreQuery.QueryType.CREATE_DOCUMENT)
                .onCollection("vendors");
        return this;
    }
    public VendorCollection(Firestore host, boolean lazyEvaluation, FirestoreQuery baseQuery) {
        super(host, lazyEvaluation, baseQuery);
    }

    public VendorCollection(Firestore host, boolean lazyEvaluation) {
        super(host, lazyEvaluation);
    }

    public VendorCollection(Firestore host) {
        super(host);
    }
    private VendorModel cleanse(FirestoreDocument doc, String name){
        VendorModel re = new VendorModel(host, lazyEvaluation);
        re.setDocumentName(name);
        Map<String, Object> fields = doc.getFields();
        try {
            re.setEmail(fields.get("email").toString());
            re.setGuid(fields.get("guid").toString());
            re.setName(fields.get("name").toString());
        } catch (ClassCastException e){
            exception = new IncorrectTypeException(e.toString());
        }
        return re;
    }
    @Override
    protected void taskResultCleanse(FirestoreTaskResult queryResult) {
        cachedValue.clear();
        for (Map.Entry<String, FirestoreDocument> E : queryResult.getDocuments().entrySet()){
            cachedValue.put(E.getKey(), cleanse(E.getValue(), E.getKey()));
        }
    }
    public void serializeSingle(VendorModel model){
        Map<String, Object> re = new HashMap<>();
        re.put("email", model.getEmail());
        re.put("guid", model.getGuid());
        re.put("name", model.getName());
        lastSerialization = re;
    }
    @Override
    protected Map<String, Object> serialize() {
        return lastSerialization;
    }
}

package models;

import com.cycastic.javabase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class LedgerCollection extends FirestoreCollectionModel<LedgerModel> {
    private Map<String, Object> lastSerialization = new HashMap<>();
    public LedgerCollection setRead(){
        baseQuery = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                .from("ledger")/*.where("stock", FirestoreQuery.Operator.GREATER_THAN, 0)*/;
        return this;
    }
    public LedgerCollection setWrite(boolean isPatching){
        baseQuery = new FirestoreQuery(isPatching ? FirestoreQuery.QueryType.PATCH_DOCUMENT : FirestoreQuery.QueryType.CREATE_DOCUMENT)
                .onCollection("ledger");
        return this;
    }
    public LedgerCollection(Firestore host, boolean lazyEvaluation, FirestoreQuery baseQuery) {
        super(host, lazyEvaluation, baseQuery);
    }

    public LedgerCollection(Firestore host, boolean lazyEvaluation) {
        super(host, lazyEvaluation);
    }

    public LedgerCollection(Firestore host) {
        super(host);
    }
    private LedgerModel cleanse(FirestoreDocument doc, String name){
        LedgerModel re = new LedgerModel(host, lazyEvaluation);
        re.setDocumentName(name);
        Map<String, Object> fields = doc.getFields();
        try {
            re.setAmount((Long) fields.get("amount"));
            re.setDvd(fields.get("dvd").toString());
            re.setInput((Boolean) fields.get("isInput"));
            re.setTime((Long) fields.get("time"));
            re.setVendor(fields.get("vendor").toString());
            re.setCommissioner(fields.get("commissioner").toString());
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
    public void serializeSingle(LedgerModel model){
        Map<String, Object> re = new HashMap<>();
        re.put("amount", model.getAmount());
        re.put("dvd", model.getDvd());
        re.put("isInput", model.getInput());
        re.put("time", model.getTime());
        re.put("vendor", model.getVendor());
        re.put("commissioner", model.getCommissioner());
        lastSerialization = re;
    }
    @Override
    protected Map<String, Object> serialize() {
        return lastSerialization;
    }
}

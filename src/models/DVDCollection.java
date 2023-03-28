package models;

import com.cycastic.javabase.firestore.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DVDCollection extends FirestoreCollectionModel<DVDModel> {
    private Map<String, Object> lastSerialization = new HashMap<>();
    public DVDCollection setRead(){
        baseQuery = new FirestoreQuery(FirestoreQuery.QueryType.STRUCTURED_QUERY)
                .from("dvds_info")/*.where("stock", FirestoreQuery.Operator.GREATER_THAN, 0)*/;
        return this;
    }
    public DVDCollection setWrite(boolean isPatching){
        baseQuery = new FirestoreQuery(isPatching ? FirestoreQuery.QueryType.PATCH_DOCUMENT : FirestoreQuery.QueryType.CREATE_DOCUMENT)
                .onCollection("dvds_info");
        return this;
    }
    public DVDCollection(Firestore host, boolean lazyEvaluation, FirestoreQuery baseQuery) {
        super(host, lazyEvaluation, baseQuery);
    }
    public DVDCollection(Firestore host, boolean lazyEvaluation) {
        super(host, lazyEvaluation);
    }
    public DVDCollection(Firestore host) {
        super(host);
    }

    private DVDModel cleanse(FirestoreDocument doc, String name){
        DVDModel re = new DVDModel(host, lazyEvaluation);
        re.setDocumentName(name);
        Map<String, Object> fields = doc.getFields();
        try {
            re.setCapsuleArt(fields.get("capsule_art").toString());
            re.setCategory((List<String>) fields.get("category"));
            re.setDirector(fields.get("director").toString());
            re.setDuration((Long) fields.get("duration"));
            re.setImdbLink(fields.get("imdb_link").toString());
            re.setName(fields.get("name").toString());
            re.setRatedAge((Long) fields.get("rated_age"));
            re.setRatedType(fields.get("rated_type").toString());
            re.setStock((Long) fields.get("stock"));
            re.setYear((Long) fields.get("year"));
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
    public void serializeSingle(DVDModel model){
        Map<String, Object> re = new HashMap<>();
        re.put("capsule_art", model.getCapsuleArt());
        re.put("category", model.getCategory());
        re.put("director", model.getDirector());
        re.put("duration", model.getDuration());
        re.put("imdb_link", model.getImdbLink());
        re.put("name", model.getName());
        re.put("rated_age", model.getRatedAge());
        re.put("rated_type", model.getRatedType());
        re.put("stock", model.getStock());
        re.put("year", model.getYear());
//        baseQuery.onDocument(model.getDocumentName());
        lastSerialization = re;
    }

    @Override
    protected Map<String, Object> serialize() {
        return lastSerialization;
    }
}

package models;

import com.cycastic.javabase.firestore.Firestore;
import com.cycastic.javabase.firestore.FirestoreModel;

import java.util.List;
import java.util.ArrayList;

public class DVDModel extends FirestoreModel {
    private String capsuleArt = "";
    private List<String> category = new ArrayList<>();
    private String director = "";
    private Long duration = 0L;
    private String imdbLink = "";
    private String name = "";
    private Long ratedAge = 0L;
    private String ratedType = "";
    private Long stock = 0L;
    private Long year = 0L;

    public DVDModel(Firestore host, boolean lazyEvaluation) {
        super(host, lazyEvaluation);
    }

    public List<String> getCategory() {
        return category;
    }

    public long getDuration() {
        return duration;
    }

    public long getRatedAge() {
        return ratedAge;
    }

    public long getStock() {
        return stock;
    }

    public long getYear() {
        return year;
    }

    public String getCapsuleArt() {
        return capsuleArt;
    }

    public String getDirector() {
        return director;
    }

    public String getImdbLink() {
        return imdbLink;
    }

    public String getName() {
        return name;
    }

    public String getRatedType() {
        return ratedType;
    }

    public void setCapsuleArt(String capsuleArt) {
        this.capsuleArt = capsuleArt;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public void setImdbLink(String imdbLink) {
        this.imdbLink = imdbLink;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRatedAge(Long ratedAge) {
        this.ratedAge = ratedAge;
    }

    public void setRatedType(String ratedType) {
        this.ratedType = ratedType;
    }

    public void setStock(Long stock) {
        this.stock = stock;
    }

    public void setYear(Long year) {
        this.year = year;
    }
}

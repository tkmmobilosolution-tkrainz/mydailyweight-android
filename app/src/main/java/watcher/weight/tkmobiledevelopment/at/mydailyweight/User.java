package watcher.weight.tkmobiledevelopment.at.mydailyweight;

/**
 * Created by tkrainz on 03/03/2017.
 */

public class User {

    private String name = "";
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    private String currentWeight = "";
    public void setCurrentWeight(String startWeight) {
        this.currentWeight  = startWeight;
    }

    public String getCurrentWeight() {
        return currentWeight;
    }

    private String dreamWeight = "";
    public void setDreamWeight(String dreamWeight) {
        this.dreamWeight = dreamWeight;
    }

    public String getDreamWeight() {
        return dreamWeight;
    }

    private String height = "";
    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }

    private String gender = "";
    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    private String age = "";
    public void setAge(String age) {
        this.age = age;
    }

    public String getAge() {
        return age;
    }
}

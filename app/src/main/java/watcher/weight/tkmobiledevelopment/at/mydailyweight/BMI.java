package watcher.weight.tkmobiledevelopment.at.mydailyweight;

/**
 * Created by tkrainz on 04/09/2017.
 */

public class BMI {

    String date = "";
    double bmi = 0;
    double weight = 0;
    int weightGroup = 0;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getBmi() {
        return bmi;
    }

    public void setBmi(double bmi) {
        this.bmi = bmi;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getWeightGroup() {
        return weightGroup;
    }

    public void setWeightGroup(int weightGroup) {
        this.weightGroup = weightGroup;
    }
}

package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by tkrainz on 10/02/2017.
 */

public class WeightView extends View {

    private static final int margin = 100;
    private Paint weightLine;
    private Paint dateLine;
    private Paint weightDevider;

    private double highestValue;
    private double lessValue;

    private boolean weightsHasDiffernts = false;

    private ArrayList<Weight> weightArrayList = new ArrayList<>();
    public void setWeightArrayList(ArrayList<Weight> list) {
        weightArrayList = list;

        if (weightArrayList.size() > 0) {
            Weight currentWeight = weightArrayList.get(0);
            highestValue = currentWeight.weightValue;

            for (int s = 1; s < weightArrayList.size(); s++){
                Weight weight = weightArrayList.get(s);
                double curValue = weight.weightValue;
                if (highestValue != curValue) {
                    weightsHasDiffernts = true;
                }
                if (curValue > highestValue) {
                    highestValue = curValue;
                }
            }

            lessValue = currentWeight.weightValue;

            for (int l = 1; l < weightArrayList.size(); l++){
                Weight weight = weightArrayList.get(l);
                double curValue = weight.weightValue;
                if (curValue < lessValue) {
                    lessValue = curValue;
                }
            }
        }
    }

    public WeightView(Context context) {
        this(context, null);
    }

    public  WeightView(Context context, AttributeSet s) {
        super(context, s);

        this.setBackgroundColor(getResources().getColor(R.color.primaryGray));

        weightLine = new Paint();
        dateLine = new Paint();
        weightDevider = new Paint();

        weightLine.setColor(getResources().getColor(R.color.primaryYellow));
        weightLine.setStrokeWidth(8);

        dateLine.setColor(Color.WHITE);
        dateLine.setStrokeWidth(2);
        dateLine.setTextSize(25);

        weightDevider.setColor(Color.WHITE);
        weightDevider.setStrokeWidth(2);
        weightDevider.setTextSize(25);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        double startX = 100;
        double endX = this.getWidth() - 20;
        double effectiveViewWidth = endX - startX;

        double stepSize = (effectiveViewWidth) / (weightArrayList.size() - 1);
        double diff = highestValue - lessValue;
        double heightStep = (this.getHeight() - margin) / diff;

        if (weightArrayList.size() == 1) {
            canvas.drawLine((float) startX, this.getHeight() / 2, (float) this.getWidth(), this.getHeight() / 2, weightLine);
            canvas.drawLine((float) startX, (float) this.getHeight() / 2, (float) this.getWidth(), (float) this.getHeight() / 2, weightDevider);
            canvas.drawText((highestValue - lessValue) / 2 + lessValue + "kg", 0, this.getHeight() / 2, weightDevider);
        } else {

            for (int i = 0; i < weightArrayList.size(); i++) {
                Weight currentWeight = weightArrayList.get(i);

                float minX = (float) (stepSize * i + startX);
                float maxX = (float) (stepSize * (i + 1) + startX);
                double currentValue = currentWeight.weightValue;
                double currentLessDiff = highestValue - currentValue;
                double nextLessDiff = currentLessDiff;

                if (i + 1 < weightArrayList.size()) {
                    Weight nextWeight = weightArrayList.get(i +1);
                    nextLessDiff = highestValue - nextWeight.weightValue;

                    float minY = (float) (currentLessDiff * heightStep) + margin / 2;
                    float maxY = (float) (nextLessDiff * heightStep) + margin / 2;

                    if (weightsHasDiffernts) {
                        canvas.drawLine(minX ,minY, maxX, maxY, weightLine);
                    } else {
                        canvas.drawLine((float) startX, this.getHeight() / 2, (float) this.getWidth(), this.getHeight() / 2, weightLine);
                    }
                }

                double calc = weightsHasDiffernts ? i % 3 : 0;
                if (calc == 0) {
                    canvas.drawLine(minX, 0, minX, (float) this.getHeight() - margin / 2, dateLine);
                    canvas.drawText(currentWeight.date, minX - 80, this.getHeight() - 20, dateLine);
                }

                if (weightsHasDiffernts) {
                    canvas.drawLine((float) startX, margin / 2, (float) this.getWidth(), margin / 2, weightDevider);
                    canvas.drawLine((float) startX, (float) this.getHeight() - margin / 2, (float) this.getWidth(), (float) this.getHeight() - margin / 2, weightDevider);
                    canvas.drawText(highestValue + "kg", 0, margin / 2, weightDevider);
                    canvas.drawText(lessValue + "kg", 0, (float) this.getHeight() - margin / 2, weightDevider);
                }
            }

            float middleWeight = (float) ((highestValue - lessValue) / 2 + lessValue);
            canvas.drawLine((float) startX, (float) this.getHeight() / 2, (float) this.getWidth(), (float) this.getHeight() / 2, weightDevider);
            canvas.drawText(String.format("%.2f", middleWeight) + "kg", 0, this.getHeight() / 2, weightDevider);
        }
    }
}

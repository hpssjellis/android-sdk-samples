package com.affectiva.framedetectordemo;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.EnumMap;

public class MetricsPanel extends Fragment {

    EnumMap<Metrics,TextView> metricsTextViews = new EnumMap<>(Metrics.class);
    int headerColor;
    int metricColor1;
    int metricColor2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.metrics_panel,container,false);
        populateMetrics(getActivity(),(LinearLayout)content.findViewById(R.id.metrics_container));
        return content;
    }

    public void setMetricValue(Metrics metric, float value) {
        metricsTextViews.get(metric).setText(String.format("%.3f", value));
    }

    public void setMetricNA(Metrics metric) {
        metricsTextViews.get(metric).setText("--");
    }

    void populateMetrics(Context context, LinearLayout container) {
        //setup Colors
        headerColor = Color.rgb(0,51,51); //Dark Teal Blue
        metricColor1 = Color.rgb(70,70,70); //Dark Gray
        metricColor2 = Color.rgb(85,85,85); //Less Dark Gray

        //Populate Emotions
        container.addView(createHeaderTextView("EMOTIONS", context));
        addSetOfMetrics(context,container,Metrics.getEmotions());

        //Populate Expressions
        container.addView(createHeaderTextView("EXPRESSIONS",context));
        addSetOfMetrics(context,container,Metrics.getExpressions());

        //Populate Measurements
        container.addView(createHeaderTextView("MEASUREMENTS", context));
        addSetOfMetrics(context,container,Metrics.getMeasurements());


    }

    void addSetOfMetrics(Context context, LinearLayout container, Metrics[] metrics) {
        boolean toggle = false;
        for (Metrics metric : metrics) {
            //Toggle Color
            int metricColor;
            toggle = !toggle;
            if (toggle)
                metricColor = metricColor1;
            else
                metricColor = metricColor2;

            //Name
            TextView nameTextView = createNameTextView(metric, context, metricColor);
            nameTextView.setBackgroundColor(metricColor);
            container.addView(nameTextView);

            //Score
            TextView metricTextView = createScoreTextView(metric,context,metricColor);
            metricTextView.setBackgroundColor(metricColor);
            metricsTextViews.put(metric, metricTextView);
            container.addView(metricTextView);
        }
    }

    private TextView createHeaderTextView(String headerName, Context context) {
        TextView header = new TextView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        header.setLayoutParams(params);
        header.setGravity(Gravity.CENTER);
        header.setText(headerName);
        header.setTextAppearance(context, R.style.metricCategory);
        header.setBackgroundColor(headerColor);
        return header;
    }

    private TextView createNameTextView(Metrics metric, Context context, int backgroundColor) {
        TextView name = new TextView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        name.setLayoutParams(params);
        name.setGravity(Gravity.CENTER);
        name.setText(metric.getUpperCaseName());
        name.setTextAppearance(context, R.style.metricName);
        return name;
    }

    private TextView createScoreTextView(Metrics metric, Context context, int backgroundColor) {
        TextView score = new TextView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        score.setLayoutParams(params);
        score.setGravity(Gravity.CENTER);
        score.setPadding(0, 0, 0, 10);
        score.setTextAppearance(context, R.style.metricScore);
        return score;
    }
}

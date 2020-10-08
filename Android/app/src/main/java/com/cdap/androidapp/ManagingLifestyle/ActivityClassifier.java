package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class ActivityClassifier {

    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "lifestyle_model/lifestyle_model.h5";
    private static final String INPUT_NODE = "x:0";
    private static final String[] OUTPUT_NODES = {"Identity:0"};
    private static final String OUTPUT_NODE = "Identity:0";
    private static final long[] INPUT_SIZE = {1, 200, 3};
    private static final int OUTPUT_SIZE = 5;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    public ActivityClassifier(Context context)
    {
        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    public float[] predict(float[] input)
    {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE, input, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE, result);
        return result;
    }

}

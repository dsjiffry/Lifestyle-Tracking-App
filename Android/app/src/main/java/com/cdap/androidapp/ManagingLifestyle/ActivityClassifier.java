package com.cdap.androidapp.ManagingLifestyle;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class ActivityClassifier {

    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "saved_model.pb";
    private static final String INPUT_NODE = "";
}

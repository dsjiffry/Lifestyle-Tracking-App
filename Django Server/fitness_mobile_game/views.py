from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from rest_framework.response import Response
from rest_framework.decorators import api_view
from rest_framework import status
import tensorflow as tf
from tensorflow import keras
import json
import os
import numpy as np

config = tf.compat.v1.ConfigProto(gpu_options = 
                         tf.compat.v1.GPUOptions(per_process_gpu_memory_fraction=0.8)
)
config.gpu_options.allow_growth = True
session = tf.compat.v1.Session(config=config)
tf.compat.v1.keras.backend.set_session(session)

# os.environ["CUDA_VISIBLE_DEVICES"]="-1" # Make predictions without using GPU
lifestyle_model = tf.keras.models.load_model('lifestyle_model.h5') #Loading up lifestyle model on start of server.

#Default return
def homePageView(request):
    return HttpResponse('Fitness Mobile Game - Backend Server')

#Make POST request to: http://127.0.0.1:8000/life
@csrf_exempt
@api_view(['POST'])
def lifestyle_management_predictions(request):
    # lifestyle_model.summary()
    jsonBody = request.data
    readings = jsonBody["data"]
    # print(request.data)

    arr = np.array(readings).astype(np.float32)
    prediction = lifestyle_model.predict(arr)
    prediction = prediction.argmax(axis=-1)

    if prediction == 0:
        activity = "jogging"
    elif prediction == 1:
        activity = "sitting"
    elif prediction == 2:
        activity = "stairs"
    elif prediction == 3:
        activity = "standing"
    elif prediction == 4:
        activity = "walking"
    else:
        activity = "unknown"


    # json = {
    #     "prediction": activity
    # }
    
    # print(prediction)



    return Response(data=activity, status=200)














#Make POST request to: http://127.0.0.1:8000/emotion
@csrf_exempt
@api_view(['POST'])
def camera_emotion_predictions(request):
    jsonBody = request.data

    ###############################################
    #Process data here
    ###############################################

    json = {
        "prediction": "testing"
    }
    return Response(data=json, status=200)
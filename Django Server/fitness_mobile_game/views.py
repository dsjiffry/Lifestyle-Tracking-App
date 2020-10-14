from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from rest_framework.response import Response
from rest_framework.decorators import api_view
from rest_framework import status
import tensorflow as tf
from tensorflow import keras
import json
import numpy as np


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
    # print(readings)

    arr = np.array(readings).astype(np.float32)
    prediction = lifestyle_model.predict(arr)
    prediction = prediction.argmax(axis=-1)

    json = {
        "prediction": prediction
    }
    
    # print(prediction)



    return Response(data=json, status=200)



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
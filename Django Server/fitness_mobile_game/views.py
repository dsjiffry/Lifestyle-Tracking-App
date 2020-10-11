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


lifestyle_model = tf.keras.models.load_model('lifestyle_model.h5')



def homePageView(request):
    return HttpResponse('Fitness Mobile Game - Backend Server')

@csrf_exempt
@api_view(['POST'])
def register(request):
    # lifestyle_model.summary()
    jsonBody = request.data
    readings = jsonBody["data"]
    # print(readings)

    arr = np.array(readings).astype(np.float32)
    prediction = lifestyle_model.predict(arr)
    # print(prediction)



    return Response(data=prediction, status=200)
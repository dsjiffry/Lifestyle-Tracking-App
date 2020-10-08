from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from rest_framework.response import Response
from rest_framework.decorators import api_view
from rest_framework import status
import tensorflow as tf
from tensorflow import keras



def homePageView(request):
    return HttpResponse('Fitness Mobile Game - Backend Server')

@csrf_exempt
@api_view(['POST'])
def register(request):
    if request.method == "POST":
        
        new_model = tf.keras.models.load_model('lifestyle_model.h5')
        new_model.summary()


        post = request.POST #if no files
    return Response(status=200)
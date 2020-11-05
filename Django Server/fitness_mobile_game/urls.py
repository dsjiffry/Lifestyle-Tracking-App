from django.urls import path
from .views import homePageView
from .views import lifestyle_management_predictions,camera_emotion_predictions

urlpatterns = [
    path('', homePageView, name='home'),
    path('life', lifestyle_management_predictions, name='life_predictions'),
    path('emotion', camera_emotion_predictions, name='emotion_predictions')
]
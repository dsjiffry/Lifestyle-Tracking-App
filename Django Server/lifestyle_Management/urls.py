from django.urls import path
from .views import homePageView
from .views import register

urlpatterns = [
    path('', homePageView, name='home'),
    path('life', register, name='life')
]
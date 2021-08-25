# Notice
My Research was done with 2 other Members, We built a Fitness Android app which would use Deep Learning Techniques in order to assist the User in maintaining their Health.
The Three Sections of the app were:
   1. Emotion Tracking
   2. Lifestyle Managing
   3. Diet Planning   

I was in charge of the Lifestyle Managing section. In my section I would take accelerometer readings from a smartwatch and by using my Deep Learning Model, predict the current Activity of the user. Then by monitoring these activities, I would assist the user in maintaining their health.   
My Section is able to identify:
   1. The user’s wake-up time and sleep time.
   2. How do they go to work (vehicle/walking)?
   3. Home and Work locations
   4. Time spent at workplace.
   5. Kind of workout they do (gym/running)
   6. What Time they work-out.

Furthermore, will Detect and notify the user of:
   1. Long sitting sessions
   2. A suitable meditating time
   3. A suitable sleeping time (to get 7 hours of sleep)

The original project is hosted at our Universities Gitlab server (http://gitlab.sliit.lk/20_21-j_19/20_21-j19),
This repo contains only my individual section. The marketing website of our project is at: https://cdap-20-21-j-19.github.io/   

# Prerequisites
   1. Python 3.7.5
   2. Android Studio
   3. SmartWatch running Wear OS 2 or greater
   4. Smartphone running Android 8.0 or greater

# Running the System
   1. Navigate Inside 'Django Server' folder
   2. Open a terminal and execute: pip install -r pip_requirements.txt
   3. Then execute: ./manage.py runserver [[Your IP Address]]:8000
   4. Open The Android folder via 'Android Studio'
   5. Edit MainActivity.java: set SERVER_BASE_URL to your (django servers) IP address
   6. Ensure that the smartphone is paired to the smartwatch.
   7. run the 'wear app' configuration on the smartwatch and the 'app' configuration on the smartphone.

# Main Research Question
Fitness is a major concern in modern society due to technological advancements making life easy. People use fitness apps to try and keep track of their health and to make their lives healthier. But a lot of fitness apps are static and do only recommend actions, they do not change with the user. Also, there is no single app that gives a fitness plan, daily routines, nutrition plan and monitors the user’s expression. Studies also show that even though health is a serious thing, when making apps they should be fun otherwise it’s hard for users to develop a habit. Another study shows that with a large number of students using social media there is an opportunity to make a fitness app that can leverage this. Since studies have shown that students who used social media more were less likely to describe their daily routines as “dreary”. More research on changing behavior has found that interventions are more likely to be effective if they include features such as goal-setting, rapid intention formation, performance, self-monitoring, individually tailored feedback, goal-reviewing, and progression. Also sharing app information on social media can result in friendly competition and camaraderie among strangers that may facilitate behavior change and improve well-being.
    
# System Architecture
When it came to building our app, we decided to have a Django server as our backend. The server would contain each of our exported models and when we send the inputs required via POST requests, the server would make the prediction and respond with the result. This would help us conserve battery life on the user’s phone since the task of making predictions would require the phone to do more work. 
![System Diagram](/System_Diagram.jpg)

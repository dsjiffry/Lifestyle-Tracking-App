#!/bin/bash 

IPADDRESS=`ipconfig | grep -Eo 'IPv4 Address.*' | grep -Eo '([0-9]*\.){3}[0-9]*'`


echo "Run this command to make Django server accessible from other devices: 

"

echo "./manage.py runserver "$IPADDRESS":8000"


sleep 10

#./manage.py runserver $IPADDRESS:8000


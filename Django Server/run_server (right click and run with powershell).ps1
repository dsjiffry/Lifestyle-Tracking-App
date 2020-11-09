
$ipAddress = (Test-Connection -ComputerName (hostname) -Count 1  | Select -ExpandProperty IPV4Address).IPAddressToString

./manage.py runserver ($ipAddress + ':8000')
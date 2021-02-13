
echo "`nGetting IP address..."

$ipAddress = (
    Get-NetIPConfiguration |
    Where-Object {
        $_.IPv4DefaultGateway -ne $null -and
        $_.NetAdapter.Status -ne "Disconnected"
    }
).IPv4Address.IPAddress

./manage.py runserver ($ipAddress + ':8000')
#!/bin/sh
docker exec iot-mysql mysql -uroot -p123456 iot_platform < /tmp/b.sql > /tmp/bulk_output.txt 2>&1
echo "Exit: $?"
cat /tmp/bulk_output.txt | tail -20

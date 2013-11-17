#!/bin/bash


echo -e "\033[1m"

if hash figlet 2>/dev/null; then
    figlet ImageMaster
else
    echo -e ImageMaster
fi

echo -e "\033[0m"

echo -e "\033[1mStaging application\033[0m"

CMD=./target/universal/stage/bin/imagemaster

sbt stage

echo -e "\033[1mInvoking master\033[0m"

$CMD  -Dhttp.port=10000 -Dpidfile.path="PID10000.pid" &

echo -e "\033[1mMaster running\033[0m"

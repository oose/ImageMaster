#!/bin/bash

CMD=./target/universal/stage/bin/imagemaster

play stage
$CMD  -Dhttp.port=10000 -Dpidfile.path="PID10000.pid" &


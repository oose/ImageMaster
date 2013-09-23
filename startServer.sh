#!/bin/bash

play stage
target/start -Dhttp.port=10000 -Dimage.dir="/Users/markusklink/Pictures/Export/BobWayne/" -Dpidfile.path="PID10000.pid" &


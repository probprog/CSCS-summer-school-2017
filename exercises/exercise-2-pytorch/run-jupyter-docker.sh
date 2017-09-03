#!/usr/bin/env bash

docker run -it --rm -p 8888:8888 -v $PWD:/workspace gbaydin/anglican-infcomp jupyter notebook --ip 0.0.0.0 --allow-root

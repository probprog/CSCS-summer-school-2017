# CSCS-summer-school-2017
CSCS-ICS-DADSi Summer School: Accelerating Data Science with HPC, September 4 – 6, 2017

## Step 1: Get the Docker image

Prerequisite: you should have [Docker](https://www.docker.com/) installed.

Start by pulling the `anglican-infcomp` image from the Docker hub:

```
docker pull gbaydin/anglican-infcomp
```

Beware: this can take some time (approximately 15 minutes over a fast LAN connection), so it might be a good idea to start pulling this image as soon as possible before the exercise sessions.


### Optional: Install `nvidia-docker`

If you have CUDA available, you can also install a Docker engine supporting NVIDIA GPUs. This will significantly speed up the neural network training phase, although the exercises we will cover already run fast enough without this.

Please follow instructions at the [nvidia-docker repository](https://github.com/NVIDIA/nvidia-docker).

## Step 2: Clone this repository 

At a location of your choosing, run 

```
git clone git@github.com:probprog/CSCS-summer-school-2017.git
```

to clone this repository containing the summer school exercises.

## Step 3: Run the Docker container

### Linux

First run

```
xhost +
```

to allow access to the X server from the docker container (this is needed for one of the exercises where a GUI is used).


Then change into the folder and start an interactive Docker container by running:

```
cd CSCS-summer-school-2017
docker run --rm -it -p 31415:31415 -v $PWD:/workspace -e DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix gbaydin/anglican-infcomp
```

### Mac

First run

```
socat TCP-LISTEN:6000,reuseaddr,fork UNIX-CLIENT:\"$DISPLAY\"
```


In a different terminal, change into the folder and start an interactive Docker container by running:

```
cd CSCS-summer-school-2017
ip=$(ifconfig en0 | grep inet | awk '$1=="inet" {print $2}')
xhost + $ip
docker run --rm -it -p 31415:31415 -v $PWD:/workspace -e DISPLAY=$ip:0 -v /tmp/.X11-unix:/tmp/.X11-unix gbaydin/anglican-infcomp
```


This will start a new Docker container using the `anglican-infcomp` image that you pulled in the previous step.

In this command `--rm` indicates that the container will be removed when it exists, `-it` attaches an interactive terminal to the container, `-p 31415:31415` sets up a port mapping for port `31415` that is used for the Gorilla REPL for Clojure, and `-v $PWD:/workspace` mounts your current folder `CSCS-summer-school-2017` as `/workspace` within the container. The flags `-e DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix` set up the X server access that is needed for one of the exercises using GUI.

If you want to run with GPU support, replace `docker` with `nvidia-docker` in the above command.

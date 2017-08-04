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

## Step 2: Clone this repository & run the Docker container

At a location of your choosing, run 

```
git clone git@github.com:probprog/CSCS-summer-school-2017.git
```

to clone this repository.

Then change into the folder and start an interactive Docker container by running:

```
cd CSCS-summer-school-2017
docker run --rm -it -v $PWD:/workspace -p 31415:31415 gbaydin/anglican-infcomp
```

This will start a new Docker container using the `anglican-infcomp` image that you pulled in the previous step and also mount your current folder `CSCS-summer-school-2017` as `/workspace` within the container.

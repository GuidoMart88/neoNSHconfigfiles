FROM ubuntu:latest
RUN apt-get update
RUN apt-get install -y python python-pip tcpdump wget nano net-tools iputils-ping
RUN pip install --upgrade pip
RUN pip install scapy
WORKDIR /home/root
COPY service_function.py /home/root

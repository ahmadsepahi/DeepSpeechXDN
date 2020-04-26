FROM ubuntu:18.04
#FROM alpine:3.11
#FROM python:3.7-alpine

RUN apt-get update
#RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-virtualenv

#ENV VIRTUAL_ENV=$HOME/tmp/deepspeech-venv/
#RUN python3 -m virtualenv --python=/usr/bin/python3 $VIRTUAL_ENV
#ENV PATH="$VIRTUAL_ENV/bin:$PATH"


#RUN apt install virtualenv
#RUN virtualenv -p python3 $HOME/tmp/deepspeech-venv/
#RUN /bin/bash -c "source $HOME/tmp/deepspeech-venv/bin/activate"
COPY SimpleHTTPServerWithUpload_3.py .
RUN apt-get install -y python3.7
RUN apt-get install -y python3-pip
RUN pip3 install deepspeech
RUN apt-get install -y curl
RUN curl -LO https://github.com/mozilla/DeepSpeech/releases/download/v0.6.1/deepspeech-0.6.1-models.tar.gz
RUN tar xvf deepspeech-0.6.1-models.tar.gz
EXPOSE 8000
CMD python3 -m SimpleHTTPServerWithUpload_3 8000

#RUN curl -LO https://github.com/mozilla/DeepSpeech/releases/download/v0.6.1/audio-0.6.1.tar.gz
#RUN tar xvf audio-0.6.1.tar.gz
#RUN deepspeech --model deepspeech-0.6.1-models/output_graph.pbmm --lm deepspeech-0.6.1-models/lm.binary --trie deepspeech-0.6.1-models/trie --audio audio/2830-3980-0043.wav
#CMD deepspeech --model deepspeech-0.6.1-models/output_graph.pbmm --lm deepspeech-0.6.1-models/lm.binary --trie deepspeech-0.6.1-models/trie --audio audio/2830-3980-0043.wav


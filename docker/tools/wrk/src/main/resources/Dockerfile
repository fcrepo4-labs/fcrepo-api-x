FROM alpine:3.4

RUN mkdir /wrk

WORKDIR /wrk

COPY wrk wrk.lua scripts/ /wrk/

ENV PATH=${PATH}:/wrk
CMD /wrk/wrk

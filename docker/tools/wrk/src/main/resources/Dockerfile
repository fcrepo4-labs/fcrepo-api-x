FROM alpine:3.4

COPY wrk wrk.lua scripts/ /wrk/

ENV PATH=${PATH}:/wrk
WORKDIR /wrk
CMD /wrk/wrk

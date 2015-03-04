FROM nightscape/docker-sbt

COPY project/build.properties project/*.sbt project/*.scala /app/project/
WORKDIR /app
RUN sbt "; update ; compile"

COPY . /app/
RUN sbt stage

CMD ["/app/server/target/universal/stage/bin/server"]
EXPOSE 8080

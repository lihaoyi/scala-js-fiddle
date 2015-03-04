Scala-Js-Fiddle
==============

Source code for [www.scala-js-fiddle.com](http://www.scala-js-fiddle.com). To develop, run:

```
sbt "~; server/re-start"
```

You can also run

```
sbt stage; ./server/target/start
```

To stage and run without SBT,

```
sbt assembly; java -jar server/target/scala-2.10/server-assembly-0.1-SNAPSHOT.jar
```

To package as a fat jar and run, or

```
capstan build -p vmw; capstan run -p vmw
```

To bundle as an image using OSv and run it under VMware, or

```
docker build -t scala-js-fiddle . ; docker run -p 8080:8080 scala-js-fiddle:latest
```
To bundle as a Docker image and run it in a Docker container.

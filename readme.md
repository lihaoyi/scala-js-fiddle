Scala-Js-Fiddle
==============

Source code for [www.scala-js-fiddle.com](http://www.scala-js-fiddle.com).
This is now obsolete, and has been superseded by Otto Chrons' 
[ScalaFiddle](https://github.com/scalafiddle).


To develop, run:

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

To bundle as an image using OSv and run it under VMware.

Scala-Js-Fiddle
==============

Source code for [www.scala-js-fiddle.com](http://www.scala-js-fiddle.com). To develop, run:

```
sbt "~; page/package; page/optimizeJS; server/re-start; client/optimizeJS"
```

This will start the server at `localhost:8080`, which you can go to and immediately start the live editing process.

When you're done with the development, you can run:

```
sbt clean compile stage; ./server/target/start
```

To stage and run the compiled code independently of SBT.

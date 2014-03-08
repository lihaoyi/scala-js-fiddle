package fiddle
import scala.scalajs.js
import js.Dynamic.{global, literal => lit}
import org.scalajs.dom
import collection.mutable
import scalatags.{HtmlTag, Modifier, Tags2}
import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import scalatags.all._

object Page{

  def body = Seq(
    pre(id:="editor"),
    pre(id:="logspam"),
    div(id:="sandbox")(
      canvas(id:="canvas", style:="position: absolute"),
      div(
        id:="output",
        color:="lightgrey",
        padding:="25px",
        boxSizing:="border-box"
      )
    )
  )

  val red = span(color:="#ffaaaa")
  val blue = span(color:="#aaaaff")
  val green = span(color:="#aaffaa")

}

import Page.{red, green, blue}

object Output{
  private[this] var outputted = div()
  def println(s: Any*) = {
    val modifier = div(s.map{
      case t: Modifier => t
      case x => x.toString: Modifier
    }:_*)
    outputted = modifier.transform(outputted)
    Client.output.innerHTML = outputted.toString()
    Client.output.scrollTop = Client.output.scrollHeight - Client.output.clientHeight
  }
  def clear(){
    outputted = div()
    Client.output.innerHTML = outputted.toString()
  }
  def scroll(px: Int){
    Client.output.scrollTop = Client.output.scrollTop + px
  }
}

object Client{

  lazy val sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  lazy val canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  lazy val renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  
  lazy val output = js.Dynamic.global.output.asInstanceOf[dom.HTMLDivElement]

  def logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]
  dom.document.body.innerHTML = Page.body.mkString

  val cloned = output.innerHTML

  class CompleteState(val row: Int,
                      val column: Int,
                      val prefix: String,
                      val suffix: String,
                      val allOptions: Seq[String],
                      var scroll: Int = 0,
                      val height: Int = 5){

    def options = allOptions.filter(_.startsWith(namePrefix))
    val pos = lit(row=row+1, column=0)

    val validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_".toSet
    val namePrefix = prefix.reverse
                           .takeWhile(validChars)
                           .reverse

    println("CompleteState " + namePrefix)
    dom.console.log(pos)
    def modulo(a: Int, b: Int) = (a % b + b) % b
    def render(): Unit = {

      val start = modulo(scroll + 1, options.length)
      val end = modulo(scroll + height, options.length)

      val sliced =
        if(end > start) options.slice(start, end)
        else options.drop(start) ++ options.take(end)

      renderSelected()

      editor.getSession().insert(
        pos,
        sliced.padTo(height, "")
              .map(" " * (column - namePrefix.length) + _ + "\n")
              .mkString
      )
    }
    def renderSelected(): Unit = {
      editor.getSession()
            .getDocument()
            .insertInLine(lit(row=row, column=column + namePrefix.length), options(modulo(scroll, options.length)).drop(namePrefix.length))
    }
    def clear(): Unit = {
      println("CLEAR CLEAR CLEAR CLEAR CLEAR CLEAR CLEAR CLEAR CLEAR CLEAR")
      println(options)
      println(modulo(scroll, options.length))

      editor.getSession()
            .getDocument()
            .removeInLine(row, column, column + options(modulo(scroll, options.length)).length)

      editor.getSession()
            .getDocument()
            .removeLines(row+1, height + row)
    }
    def update(): Unit = {

      clear()
      render()
    }
  }

  var autocompleted: Option[CompleteState] = None

  lazy val editor: js.Dynamic = {
    val editor = global.ace.edit("editor")
    editor.setTheme("ace/theme/twilight")
    editor.getSession().setMode("ace/mode/scala")
    editor.renderer.setShowGutter(false)

    val bindings = Seq(
      ("Compile", "Enter", compile _),
      ("Save", "S", save _),
      ("Complete", "`", complete _)
    )

    for ((name, key, func) <- bindings){
      editor.commands.addCommand(lit(
        name = name,
        bindKey = lit(
          win = "Ctrl-" + key,
          mac = "Command-" + key,
          sender = "editor|cli"
        ),
        exec = func: js.Function0[_]
      ))
    }

    val orig = editor.keyBinding.onCommandKey.bind(editor.keyBinding)
    editor.keyBinding.onCommandKey = {(e: js.Dynamic, hashId: js.Dynamic, keyCode: js.Number) =>
      println("Intercept! " + keyCode)
      (autocompleted, keyCode) match{
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.escape =>
          a.clear()
          autocompleted = None
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.down =>
          a.clear()
          a.scroll += 1
          a.render()
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.up =>
          a.clear()
          a.scroll -= 1
          a.render()
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.enter =>
          a.clear()
          a.renderSelected()
          autocompleted = None
          e.preventDefault()
        case _ =>
          orig(e, hashId, keyCode)
      }
    }: js.Function3[js.Dynamic, js.Dynamic, js.Number, _]

    editor.getSession().setTabSize(2)
    js.Dynamic.global.ed = editor
    editor
  }

  def clear() = {
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    Output.clear()
    output.innerHTML = cloned
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
  }
  val fiddleUrl = "http://www.scala-js-fiddle.com"
  val saved = mutable.Map.empty[String, String]
  var logged = div(
    div("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-S"), " to save to a Gist"),
    div("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")
  )
  def logln(s: Modifier*): Unit = {
    log(div(s:_*))
  }

  def log(s: Modifier*): Unit = {
    logged = s.foldLeft(logged)((a, b) => b.transform(a))
    logspam.innerHTML = logged.toString()
    logspam.scrollTop = logspam.scrollHeight - logspam.clientHeight
  }
  val (defaultGistId, defaultFile) = ("9405209", "LandingPage.scala")

  def main(args: Array[String]): Unit = {
    clear()
    if (dom.document.location.pathname == "/") load(defaultGistId, Some(defaultFile))
    else {
      val path = dom.document.location.pathname.drop("/gist/".length)
      val parts = path.split("/")
      load(
        parts(0), parts.lift(1)
      )
    }
  }
  def load(gistId: String, file: Option[String]): Unit = async {

    val gistUrl = "https://gist.github.com/" + gistId

    logln(
      "Loading ",
      file.fold(span)(s => span(
        a(href:=gistUrl + "#file-" + s.toLowerCase.replace('.', '-'))(s),
        " from "
      )),
      a(href:=gistUrl)(gistUrl),
      "..."
    )

    val res = await(Ajax.get("https://api.github.com/gists/" + gistId))
    val result = js.JSON.parse(res.responseText)

    val allFiles = result.files.asInstanceOf[js.Dictionary[js.Dynamic]]
    val mainFile = allFiles(file.getOrElse(""): String)

    val firstFile = allFiles(js.Object.keys(allFiles)(0).toString)
    val content = (if (!mainFile.isInstanceOf[js.Undefined]) mainFile else firstFile).selectDynamic("content").toString

    editor.getSession().setValue(content)
    saved(content) = gistId
    compile()
  }.recover{ case e =>
    logln(red(s"Loading failed with $e, Falling back to default example."))
    load(defaultGistId, Some(defaultFile))
  }

  def compile(): Future[Unit] = async {

    val code = editor.getSession().getValue().asInstanceOf[String]
    log("Compiling... ")
    val res = await(Ajax.post("/compile", code))

    val result = js.JSON.parse(res.responseText)
    if (result.logspam.toString != ""){
      logln(result.logspam.toString)
    }
    if(result.success.asInstanceOf[js.Boolean]){
      clear()
      js.eval(""+result.code)
      log(green("Success"))
    }else{
      log(red("Failure"))
    }
    logln()
  }.recover{ case e =>
    e.printStackTrace()
    e
  }

  def complete() = async {
    if (autocompleted == None){
      val Seq(row, column) = Seq(
        editor.getCursorPosition().row,
        editor.getCursorPosition().column
      ).map(_.asInstanceOf[js.Number].toInt)

      val code = editor.getSession().getValue().asInstanceOf[String]
      val intOffset = code.split("\n").take(row).map(_.length + 1).sum + column

      logln("Completing...")

      val xhr = await(Ajax.post("/complete/" + intOffset, code))
      val result = js.JSON.parse(xhr.responseText).asInstanceOf[js.Array[String]]
      val line = editor.getSession().getDocument().getLine(row).asInstanceOf[js.String]
      if (result.toSeq.length > 0){
        autocompleted = Some(new CompleteState(
          row,
          column,
          line.take(column),
          line.drop(column),
          result.toSeq
        ))
        autocompleted.get.render()
      }
    }
  }
  def save(): Unit = async{
    await(compile())
    val code = editor.getSession().getValue().asInstanceOf[String]
    val resultId = saved.lift(code) match{
      case Some(id) => id
      case None =>
        val res = await(Ajax.post("https://api.github.com/gists",
          data = js.JSON.stringify(
            lit(
              description = "Scala.jsFiddle gist",
              public = true,
              files = js.Dictionary(
                ("Main.scala": js.String) -> lit(
                  content = code
                )
              )
            )
          )
        ))
        val result = js.JSON.parse(res.responseText)
        saved(code) = result.id.toString
        result.id
    }

    val fiddleUrl = dom.document.location.origin + "/gist/" + resultId
    logln("Saved as ", a(href:=fiddleUrl)(fiddleUrl))
    dom.history.pushState(null, null, "/gist/" + resultId)
    val gistUrl = "https://gist.github.com/" + resultId
    logln("Or view on github at ", a(href:=gistUrl)(gistUrl))
  }
}

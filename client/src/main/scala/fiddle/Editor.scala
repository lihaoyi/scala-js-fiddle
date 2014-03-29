package fiddle
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import js.Dynamic.{literal => lit}
import js.{Dynamic => Dyn}
import scala.scalajs.js.Dynamic._
import scala.Some
import org.scalajs.dom
import rx._
import JsVal.jsVal2jsAny
import org.scalajs.dom.extensions.KeyCode
import scala.concurrent.Future
import scala.async.Async.{async, await}

class Editor(bindings: Seq[(String, String, () => Any)],
             completions: () => Future[Seq[String]]){

  def sess = editor.getSession()
  def aceDoc = sess.getDocument()
  def code = sess.getValue().asInstanceOf[String]
  println("Editor.<init>")
  def row = editor.getCursorPosition().row.asInstanceOf[js.Number].toInt
  def column= editor.getCursorPosition().column.asInstanceOf[js.Number].toInt

  def line = aceDoc.getLine(row)
                   .asInstanceOf[js.String]

  val editor: js.Dynamic = {
    val editor = Page.initEditor

    for ((name, key, func) <- bindings){
      editor.commands.addCommand(JsVal.obj(
        "name" -> name,
        "bindKey" -> JsVal.obj(
          "win" -> ("Ctrl-" + key),
          "mac" -> ("Command-" + key),
          "sender" -> "editor|cli"
        ),
        "exec" -> func
      ))
    }
    val langTools = js.Dynamic.global.require("ace/ext/language_tools")

    editor.setOptions(JsVal.obj("enableBasicAutocompletion" -> true));

    editor.completers = js.Array(lit(
      getCompletions= (editor: Dyn, session: Dyn, pos: Dyn, prefix: Dyn, callback: Dyn) => task*async{

        val things = await(completions()).map(name =>
          lit(name=name, value=name, score=10000000, meta="meta")
        )
        callback(null, js.Array(things:_*))
      }
    ))

    editor.getSession().setTabSize(2)
    editor
  }
}
package fiddle
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import js.Dynamic.{literal => lit}
import scala.scalajs.js.Dynamic._
import scala.Some
import org.scalajs.dom
import rx._
import JsVal.jsVal2jsAny
import org.scalajs.dom.extensions.KeyCode

class Editor(autocompleted: Var[Option[Completer]], bindings: Seq[(String, String, () => Any)]){

  def sess = editor.getSession()
  def aceDoc = sess.getDocument()
  def code = sess.getValue().asInstanceOf[String]
  println("Editor.<init>")
  val rowCol = Rx{
    println("Editor.rowCol")
    val Seq(newRow, newColumn) = Seq(
      editor.getCursorPosition().row,
      editor.getCursorPosition().column
    ).map(_.asInstanceOf[js.Number].toInt)
    (newRow, newColumn)
  }

  def line = aceDoc.getLine(rowCol()._1)
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

    val orig = editor.keyBinding.onCommandKey.bind(editor.keyBinding)

    editor.on("click", () => Util.defer{
      autocompleted().foreach(_.clear())
      autocompleted() = None
    })

    editor.selection.on("changeCursor", () => rowCol.recalc())

    editor.keyBinding.onCommandKey = { (e: js.Dynamic, hashId: js.Dynamic, keyCode: js.Number) =>
      println("Editor.onCommandKey")
      autocompleted().fold[Unit](orig(e, hashId, keyCode)){ a =>
        keyCode.toInt match {
          case KeyCode.escape | KeyCode.enter =>
            a.clear()
            autocompleted() = None
            e.preventDefault()
          case KeyCode.down => a.scroll() = a.scroll() + 1
          case KeyCode.up => a.scroll() = a.scroll() - 1
          case x =>
            orig(e, hashId, keyCode)
        }
      }
    }

    editor.getSession().setTabSize(2)
    editor
  }
}
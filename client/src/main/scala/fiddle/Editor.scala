package fiddle
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import js.Dynamic.{literal => lit}
import scala.scalajs.js.Dynamic._
import scala.Some
import org.scalajs.dom
import rx._
import JsVal.jsVal2jsAny

class Editor(autocompleted: Var[Option[Completer]], bindings: Seq[(String, String, () => Any)]){

  def sess = editor.getSession()
  def aceDoc = sess.getDocument()
  def code = sess.getValue().asInstanceOf[String]
  def getRowCol = {
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

    editor.on("click", () => dom.setTimeout(
      {() =>
        rowCol() = getRowCol
        autocompleted().foreach(_.killOrUpdate())
      },
      1
    ))
    editor.keyBinding.onCommandKey = {(e: js.Dynamic, hashId: js.Dynamic, keyCode: js.Number) =>
      rowCol() = getRowCol
      (autocompleted(), keyCode) match{
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.escape =>
          a.clearAll()
          autocompleted() = None
          a.options.kill()
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.down =>
          a.clearAll()
          a.selected() = a.options()((a.scroll() + 1) % a.options().length)
          a.renderAll()
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.up =>
          a.clearAll()
          a.selected() = a.options()((a.scroll() - 1 + a.options().length) % a.options().length)
          a.renderAll()
        case (Some(a), x) if x.toInt == dom.extensions.KeyCode.enter =>
          a.clear()
          autocompleted() = None
          a.options.kill()
          e.preventDefault()
        case (Some(a), x)
          if Completer.validIdentChars(js.String.fromCharCode(x.toInt).toString()(0)) =>
          val (row, column) = rowCol()

          aceDoc.removeInLine(row, column, column + 1)

          dom.setTimeout(
            () => {
              rowCol() = getRowCol
              a.clearAll()
              a.renderAll()
            },
            0
          )

        case (Some(a), x) =>
          orig(e, hashId, keyCode)
          dom.setTimeout(
            () => {
              rowCol() = getRowCol
              a.killOrUpdate()
            },
            0
          )

        case _ =>
          orig(e, hashId, keyCode)
      }
    }

    editor.getSession().setTabSize(2)
    js.Dynamic.global.ed = editor
    editor
  }
  val rowCol = Var{ getRowCol }
}
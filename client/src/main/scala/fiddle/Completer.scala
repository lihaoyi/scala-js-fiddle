package fiddle
import rx._
import scala.scalajs.js.Dynamic.{literal => lit, _}
object Completer {
  val validIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_~!#$%^&*-+=|:<>?/0123456789".toSet
  def startColumn(line: String, column: Int) = {
    line.take(column).reverse.dropWhile(validIdentChars).length
  }
  def endColumn(line: String, column: Int) = {
    column + line.drop(column).takeWhile(validIdentChars).length
  }
}
class Completer(editor: Editor,
                val selected: Var[String],
                row: Int,
                column: Int,
                allOptions: List[String],
                kill: () => Unit,
                height: Int = 5){


  lazy val scroll = Rx{
    options().indexWhere(_.toLowerCase().==(selected().toLowerCase()))
  }

  lazy val options = Rx{
    val (_, newColumn) = editor.rowCol()

    allOptions.filter(_.startsWith(editor.line.substring(column, newColumn)))
  }

  val pos = lit(row=row+1, column=0)

  def endColumn = Completer.endColumn(editor.line, column)

  def modulo(a: Int, b: Int) = (a % b + b) % b

  def renderAll() = {
    render()
    renderSelected()
  }

  def render(): Unit = {

    val start = modulo(scroll() + 1, options().length)
    val end = modulo(scroll() + height, options().length)

    val sliced =
      if(end > start) options().slice(start, end)
      else options().drop(start) ++ options().take(end)

    editor.sess.insert(
      pos,
      sliced.padTo(height, "")
            .map(" " * column + _ + "\n")
            .mkString
    )
  }
  def renderSelected(): Unit = {
    val (newRow, newColumn) = editor.rowCol()

    if (!options().isEmpty){

      editor.aceDoc.insertInLine(
        lit(row=row, column=newColumn),
        options()(modulo(scroll(), options().length)).drop(newColumn - column)
      )
    }

    editor.sess.getSelection().selectionLead.setPosition(newRow, newColumn)
  }

  def clearAll() = {
    clear()
    clearSelected()
  }

  def clear(): Unit = {
    editor.aceDoc.removeLines(row+1, height + row)
  }

  def clearSelected(): Unit = {
    val (_, newColumn) = editor.rowCol()
    if (!options().isEmpty){
      editor.aceDoc.removeInLine(row, newColumn, Completer.endColumn(editor.line, newColumn))
    }
  }

  def killOrUpdate(): Unit = {
    val (newRow, newColumn) = editor.rowCol()

    clearAll()
    if (newRow != row || Completer.startColumn(editor.line, newColumn) != column) {
      kill()
    } else {
      renderAll()
    }
  }
}
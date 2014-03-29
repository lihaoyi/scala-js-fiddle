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
                initialSelected: String,
                row: Int,
                column: Int,
                allOptions: List[String],
                kill: () => Unit,
                height: Int = 5){

  val options = Rx{
    val (_, newColumn) = editor.rowCol()

    allOptions.filter(_.startsWith(editor.line.substring(column, newColumn)))
  }

  val scroll = Var{
    options().indexWhere(_.toLowerCase == initialSelected)
  }

  val start = Rx{modulo(scroll() + 1, options().length)}
  val end = Rx{modulo(scroll() + height, options().length)}

  val sliced = Rx{
    if(end() > start()) options().slice(start(), end())
    else options().drop(start()) ++ options().take(end())
  }

  val renderer = Obs(sliced, skipInitial = true){
    clear()
    render()
  }

  render()

  def clear() = {
    val (newRow, newColumn) = editor.rowCol()
    editor.aceDoc.removeLines(row+1, height + row)

    if (!options().isEmpty){
      editor.aceDoc.removeInLine(row, newColumn, Completer.endColumn(editor.line, newColumn))
    }
  }

  def render() = {
    val (newRow, newColumn) = editor.rowCol()
    editor.sess.insert(
      pos,
      sliced().padTo(height, "")
        .map(" " * column + _ + "\n")
        .mkString
    )

    if (!options().isEmpty){

      editor.aceDoc.insertInLine(
        lit(row=row, column=newColumn),
        options()(modulo(scroll(), options().length)).drop(newColumn - column)
      )
    }

    editor.sess.getSelection().selectionLead.setPosition(newRow, newColumn)
  }

  val pos = lit(row=row+1, column=0)

  def endColumn = Completer.endColumn(editor.line, column)

  def modulo(a: Int, b: Int) = (a % b + b) % b

}
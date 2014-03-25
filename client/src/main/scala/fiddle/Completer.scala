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
class Completer(client: Client,
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
    val (_, newColumn) = client.editor.rowCol()

    allOptions.filter(_.startsWith(client.editor.line.substring(column, newColumn)))
  }

  val pos = lit(row=row+1, column=0)

  def endColumn = Completer.endColumn(client.editor.line, column)

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

    client.editor.sess.insert(
      pos,
      sliced.padTo(height, "")
            .map(" " * column + _ + "\n")
            .mkString
    )
  }
  def renderSelected(): Unit = {
    val (newRow, newColumn) = client.editor.rowCol()

    if (!options().isEmpty){

      client.editor.aceDoc.insertInLine(
        lit(row=row, column=newColumn),
        options()(modulo(scroll(), options().length)).drop(newColumn - column)
      )
    }

    client.editor.sess.getSelection().selectionLead.setPosition(newRow, newColumn)
  }

  def clearAll() = {
    clear()
    clearSelected()
  }

  def clear(): Unit = {
    client.editor.aceDoc.removeLines(row+1, height + row)
  }

  def clearSelected(): Unit = {
    val (_, newColumn) = client.editor.rowCol()
    if (!options().isEmpty){
      client.editor.aceDoc.removeInLine(row, newColumn, Completer.endColumn(client.editor.line, newColumn))
    }
  }

  def killOrUpdate(): Unit = {
    val (newRow, newColumn) = client.editor.rowCol()

    clearAll()
    if (newRow != row || Completer.startColumn(client.editor.line, newColumn) != column) {
      kill()
    } else {
      renderAll()
    }
  }
}
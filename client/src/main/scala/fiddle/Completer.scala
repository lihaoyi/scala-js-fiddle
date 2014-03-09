package fiddle

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => lit, _}
import scala.async.Async._
import collection.mutable
object Completer {
  val validIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_".toSet
  def startColumn(line: String, column: Int) = {
    line.take(column).reverse.dropWhile(validIdentChars).length
  }
  def endColumn(line: String, column: Int) = {
    line.drop(column).takeWhile(validIdentChars).length
  }
}
class Completer(row: Int,
                column: Int,
                aceDoc: js.Dynamic,
                editor: js.Dynamic,
                allOptions: List[String],
                kill: () => Unit,
                height: Int = 5){

  var scroll: Int = 0
  def options = {
    val Seq(newRow, newColumn) = Seq(
      editor.getCursorPosition().row,
      editor.getCursorPosition().column
    ).map(_.asInstanceOf[js.Number].toInt)

    allOptions.filter(_.startsWith(line.substring(column, newColumn)))
  }
  val pos = lit(row=row+1, column=0)

  def endColumn = Completer.endColumn(line, column)

  def line = aceDoc.getLine(row).asInstanceOf[js.String]

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
            .map(" " * column + _ + "\n")
            .mkString
    )
  }
  def renderSelected(): Unit = {
    val Seq(newRow, newColumn) = Seq(
      editor.getCursorPosition().row,
      editor.getCursorPosition().column
    ).map(_.asInstanceOf[js.Number].toInt)

    if (!options.isEmpty)
      println("renderSelected " + row + " " + column + " " + options(modulo(scroll, options.length)).drop(newColumn - column))
      aceDoc.insertInLine(
        lit(row=row, column=newColumn),
        options(modulo(scroll, options.length)).drop(newColumn - column)
      )
    editor.getSession().getSelection().selectionLead.setPosition(newRow, newColumn)
  }
  def clear(): Unit = {
    val Seq(newRow, newColumn) = Seq(
      editor.getCursorPosition().row,
      editor.getCursorPosition().column
    ).map(_.asInstanceOf[js.Number].toInt)

    if (!options.isEmpty){
      println(s"clear Selected $newColumn ${column - newColumn + options(modulo(scroll, options.length)).length}" )
      aceDoc.removeInLine(row, newColumn, column - newColumn + options(modulo(scroll, options.length)).length)
    }
    aceDoc.removeLines(row+1, height + row)
  }
  def calc(): Unit = {
    println("calc")
    val Seq(newRow, newColumn) = Seq(
      editor.getCursorPosition().row,
      editor.getCursorPosition().column
    ).map(_.asInstanceOf[js.Number].toInt)

    if (newRow != row || Completer.startColumn(line, newColumn) != column) {
      println("calc A")
      clear()
      kill()
    } else {
      println("calc B")
      clear()
      render()
    }
  }
  def update(): Unit = {

    clear()
    render()
  }
}
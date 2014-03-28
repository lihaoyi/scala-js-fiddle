package fiddle
import scala.reflect.macros.Context
import scala.language.experimental.macros

/**
 * Macro-powered print/println/render/renderln that takes Any* and stringifies
 * all arguments to String* before handing them over the equivalent Page.XXX,
 * since Page.XXX is separately compiled and optimized and only primitive JS
 * values can be reliably passed in.
 */
object Macros {

  def doPrint(s: String)(c: Context)(exprs: c.Expr[Any]*): c.Expr[Unit] = {
    import c.universe._
    val strExprs = exprs.map(e => c.resetLocalAttrs(q"""($e).toString"""))
    val name = newTermName(s)
    c.Expr[Unit](q"""Page.$name(..$strExprs)""")
  }
  def printlnProxy(c: Context)(exprs: c.Expr[Any]*): c.Expr[Unit] = doPrint("println")(c)(exprs:_*)
  def println(exprs: Any*): Unit = macro printlnProxy
  def renderlnProxy(c: Context)(exprs: c.Expr[Any]*): c.Expr[Unit] = doPrint("renderln")(c)(exprs:_*)
  def renderln(exprs: Any*): Unit = macro renderlnProxy
  def printProxy(c: Context)(exprs: c.Expr[Any]*): c.Expr[Unit] = doPrint("print")(c)(exprs:_*)
  def print(exprs: Any*): Unit = macro printlnProxy
  def renderProxy(c: Context)(exprs: c.Expr[Any]*): c.Expr[Unit] = doPrint("render")(c)(exprs:_*)
  def render(exprs: Any*): Unit = macro renderlnProxy
}

package fiddle
import acyclic.file
import org.scalajs.dom
import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import js.Dynamic.{literal => lit}

/**
 * Things that should eventually be pushed upstream to one of the libraries
 * that scala-js-fiddle depends on.
 */
object Util {
  object Form{
    def post(path: String, args: (String, String)*): Unit = {
      ajax("post", path, args:_*)
    }
    def get(path: String, args: (String, String)*): Unit = {
      ajax("get", path, args:_*)
    }
    def ajax(method: String, path: String, args: (String, String)*): Unit = {
      val form = dom.document.createElement("form").asInstanceOf[dom.HTMLFormElement]
      form.setAttribute("method", method)
      form.setAttribute("action", path)

      for((k, v) <- args){
        val hiddenField = dom.document.createElement("input")
        hiddenField.setAttribute("type", "hidden")
        hiddenField.setAttribute("name", k)
        hiddenField.setAttribute("value", v)
        form.appendChild(hiddenField)
      }

      dom.document.body.appendChild(form)
      form.submit()
    }
  }

  /**
   * Creates a HTML node from the given string
   */
  def createDom(s: String) = {
    val parser = new dom.DOMParser
    dom.document.adoptNode(
      parser.parseFromString(s, "text/html").documentElement
    ).lastChild.lastChild
  }
}

case class Channel[T](){
  private[this] var value: Promise[T] = null
  def apply(): Future[T] = {
    value = Promise[T]()
    value.future
  }
  def update(t: T) = {
    if (!value.isCompleted) value.success(t)
  }
}

class JsVal(val value: js.Dynamic) {
  def get(name: String): Option[JsVal] = {
    (value.selectDynamic(name): js.Any) match {
      case _: js.Undefined => None
      case v => Some(JsVal(v))
    }
  }
  def apply(name: String): JsVal = get(name).get
  def apply(index: Int): JsVal = value.asInstanceOf[js.Array[JsVal]](index)

  def keys: Seq[String] = js.Object.keys(value.asInstanceOf[js.Object]).toSeq.map(x => x: String)
  def values: Seq[JsVal] = keys.toSeq.map(x => JsVal(value.selectDynamic(x)))

  def isDefined: Boolean = !(value: js.Any).isInstanceOf[js.Undefined]
  def isNull: Boolean = value eq null

  def asDouble: Double = value.asInstanceOf[js.Number]
  def asBoolean: Boolean = value.asInstanceOf[js.Boolean]
  def asString: String = value.asInstanceOf[js.String]

  override def toString(): String = js.JSON.stringify(value)
}

object JsVal {
  def parse(value: String) = new JsVal(js.JSON.parse(value))
  def apply(value: js.Any) = new JsVal(value.asInstanceOf[js.Dynamic])
  def obj(keyValues: (String, Any)*) = {
    val obj = new js.Object().asInstanceOf[js.Dynamic]
    for ((k, v) <- keyValues){
      obj.updateDynamic(k)(v.asInstanceOf[js.Any])
    }
    new JsVal(obj)
  }
  def arr(values: Any*) = {
    new JsVal((values.toArray[Any]: js.Array[Any]).asInstanceOf[js.Dynamic])
  }
}

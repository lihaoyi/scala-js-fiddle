package fiddle

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import org.scalajs.dom


case class Channel[T](){
  private[this] var value: Promise[T] = null
  def apply(): Future[T] = {
    value = Promise[T]()
    value.future
  }
  def update(t: T) = {
    if (value != null && !value.isCompleted) value.success(t)
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
  implicit def jsVal2jsAny(v: JsVal): js.Any = v.value

  implicit def jsVal2String(v: JsVal): js.Any = v.toString
  def parse(value: String) = new JsVal(js.JSON.parse(value))
  def apply(value: js.Any) = new JsVal(value.asInstanceOf[js.Dynamic])
  def obj(keyValues: (String, js.Any)*) = {
    val obj = new js.Object().asInstanceOf[js.Dynamic]
    for ((k, v) <- keyValues){
      obj.updateDynamic(k)(v.asInstanceOf[js.Any])
    }
    new JsVal(obj)
  }
  def arr(values: js.Any*) = {
    new JsVal((values.toArray[js.Any]: js.Array[js.Any]).asInstanceOf[js.Dynamic])
  }
}


/**
 * Used to mark a Future as a task which returns Unit, making
 * sure to print the error and stack trace if it fails.
 */
object task{
  def *[T](f: Future[T])(implicit ec: ExecutionContext) = {
    f.map(_ => ()).recover{ case e =>
      println(e)
      e.printStackTrace()
    }
  }
}

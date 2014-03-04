package fiddle

import org.scalajs.dom
import scala.scalajs.js
import scala.concurrent.{Promise, Future}

case class AjaxException(xhr: dom.XMLHttpRequest) extends Exception
object Ajax{
  def get(url: String,
          data: String = "",
          timeout: Int = 0,
          headers: Seq[(String, String)] = Nil,
          withCredentials: Boolean = false) = {
    apply("GET", url, data, timeout, headers, withCredentials)
  }
  def post(url: String,
           data: String = "",
           timeout: Int = 0,
           headers: Seq[(String, String)] = Nil,
           withCredentials: Boolean = false) = {
    apply("POST", url, data, timeout, headers, withCredentials)
  }
  def apply(method: String,
            url: String,
            data: js.String,
            timeout: Int,
            headers: Seq[(String, String)],
            withCredentials: Boolean): Future[dom.XMLHttpRequest] = {
    val req = new dom.XMLHttpRequest()
    val promise = Promise[dom.XMLHttpRequest]
    req.withCredentials = withCredentials

    req.onreadystatechange = {(e: dom.Event) =>
      if (req.readyState.toInt == 4){
        if (200 <= req.status && req.status < 300)
          promise.success(req)
        else
          promise.failure(AjaxException(req))
      }
    }
    req.open(method, url)
    headers.foreach(x => req.setRequestHeader(x._1, x._2))
    req.send(data)
    promise.future
  }
}
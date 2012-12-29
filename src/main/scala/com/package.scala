import spray.json.DefaultJsonProtocol

package object com extends DefaultJsonProtocol {

  implicit val msg = new DefaultJsonProtocol{}.jsonFormat3(framework.CometMessage)

}

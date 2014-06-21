/**
 * Created by haoyi on 6/21/14.
 */
package object fiddle {
  implicit class Pipeable[T](t: T){
    def |>[V](f: T => V): V = f(t)
  }
}

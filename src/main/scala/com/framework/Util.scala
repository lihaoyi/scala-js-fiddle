package com.framework

import java.io.InputStream
import io.Source


object Util {
  def loadResource(path: String): InputStream = {
    getClass.getResourceAsStream(s"/$path")
  }
  def loadString(path: String): String = {
    Source.fromInputStream(loadResource(path)).mkString
  }
}

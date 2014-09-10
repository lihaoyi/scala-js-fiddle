package fiddle
import acyclic.file
import scalatags.Text.all._
import scalatags.Text.tags2
object Static{
  val aceFiles = Seq(
    "/META-INF/resources/webjars/ace/01.08.2014/src-min/ace.js",
    "/META-INF/resources/webjars/ace/01.08.2014/src-min/ext-language_tools.js",
    "/META-INF/resources/webjars/ace/01.08.2014/src-min/ext-static_highlight.js",
    "/META-INF/resources/webjars/ace/01.08.2014/src-min/mode-scala.js",
    "/META-INF/resources/webjars/ace/01.08.2014/src-min/theme-twilight.js"
  )

  def page(arg: String, srcFiles: Seq[String], source: String = "", compiled: String = "", analytics: Boolean = true) =
    "<!DOCTYPE html>" + html(
      head(
        meta(charset:="utf-8"),
        tags2.title("Scala-Js-Fiddle"),

        for(srcFile <- srcFiles ++ aceFiles) yield script(
          `type`:="text/javascript", src:=srcFile
        ),
        link(rel:="stylesheet", href:="/META-INF/resources/webjars/normalize.css/2.1.3/normalize.css"),
        link(rel:="stylesheet", href:="/styles.css"),

        if (analytics) script(raw(
          """
            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
              m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
              })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

              ga('create', 'UA-27464920-3', 'scala-js-fiddle.com');
              ga('send', 'pageview');
          """
        )) else ()

      ),
      body(
        div(id:="source", display:="none")(source),
        pre(id:="editor"),
        pre(id:="logspam"),
        div(id:="sandbox")(
          canvas(id:="canvas", style:="position: absolute"),
          div(
            id:="output",
            color:="lightgrey",
            paddingLeft:="2px",
            boxSizing:="border-box"
          )(
            div(id:="spinner-holder")(
              div(display:="table-cell", verticalAlign:="middle", height:="100%")(
                div(style:="text-align: center")(
                  h1("Loading Scala-Js-Fiddle"),
                  div(
                    img(src:="/Shield.svg", height:="200px")
                  ),
                  br,
                  div(
                    img(src:="/spinner.gif")
                  ),
                  p("This takes a while the first time. Please be patient =)")
                )
              )
            )
          )
        )
      ),
      script(
        id:="compiled",
        raw(compiled)
      ),
      script(raw(arg))
    ).toString()
}

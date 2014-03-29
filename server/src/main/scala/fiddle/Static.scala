package fiddle
import acyclic.file
import scalatags._
import scalatags.all._
object Static{


  def page(arg: String, srcFile: String, source: String = "", compiled: String = "", analytics: Boolean = true) =
    "<!DOCTYPE html>" + html(
      head(
        meta(charset:="utf-8"),
        Tags2.title("Scala-Js-Fiddle"),
        script(src:="/ace/ace.js", `type`:="text/javascript", charset:="utf-8"),
        script(src:="/ace/ext-language_tools.js", `type`:="text/javascript", charset:="utf-8"),
        link(rel:="stylesheet", href:="/normalize.css"),
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
        ),
//        script(`type`:="text/javascript", src:="/example-extdeps.js"),
//        script(`type`:="text/javascript", src:="/example-intdeps.js"),
//        script(`type`:="text/javascript", src:="/example.js"),
        script(`type`:="text/javascript", src:=srcFile),
        script(s"Page2=Page();", raw(arg)),
        script(id:="compiled", raw(compiled))
      )
    ).toString()
}

package com
package site

import rx.{Sig, Var}
import scalatags.ScalaTags._

import xml.Unparsed
import io.Source
import java.io.InputStream
import framework.Util


class Page(val id: String, var path: String) extends com.framework.Page{

  val (parser, initFile, highlighter) = {
    import org.pegdown.Extensions._
    import org.pegdown._
    import net.java.textilej

    path match{
      case "/html" =>
        ((x: String) => x, "landing.html", "html")
      case "/textile" =>
        val p = new textilej.parser.MarkupParser(new textilej.parser.markup.textile.TextileDialect())
        ((x: String) => p.parseToHtml(x), "landing.textile", "textile")
      case "/mediawiki" =>
        val p = new textilej.parser.MarkupParser(new textilej.parser.markup.mediawiki.MediaWikiDialect())
        ((x: String) => p.parseToHtml(x), "landing.mediawiki", "mediawiki")
      case "/github" =>
        val p = new PegDownProcessor(HARDWRAPS | AUTOLINKS | FENCED_CODE_BLOCKS)
        ((x: String) => p.markdownToHtml(x), "landing.github", "markdown")
      case "/markdown" | _ =>
        val p = new PegDownProcessor()
        ((x: String) => p.markdownToHtml(x), "landing.markdown", "markdown")
    }
  }

  val textInput = Var(Util.loadString("landings/" + initFile))

  val parsed = Sig{
    println("in: " + textInput().length)
    val out = parser(textInput())
    println("out: " + out.length)
    out
  }

  val view =
    html.h("100%").attr("lang" -> "en").overflow("hidden")(
      head(
        script.src("//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js")(""),
        pageHeader,

        script.src("//d1n0x3qji82z53.cloudfront.net/src-min-noconflict/ace.js")(""),
        link.href("/css/bootstrap.css").ctype("text/css").rel("stylesheet")

      ),
      body.h("100%").w("100%").css("margin" -> "0px")(
        div.w("100%").h("100%").id("holder")(
          div.fL.w("50%").h("100%").id("editor")(

            textInput.now()
          ),script(
            stash(x => s"""
              var editor = ace.edit("editor")
              editor.commands.bindKeys({"ctrl-r":null})
              editor.setTheme("ace/theme/clouds")
              editor.getSession().setMode("ace/mode/$highlighter")
              editor.getSession().setUseWrapMode(true)
              editor.renderer.setShowGutter(false)
              var clientHeight = function(){
                return $$("#holder")[0].clientHeight
              }
              var editorHeight = function(){
                return editor.getSession().getScreenLength() * editor.renderer.lineHeight
              }
              var scrollPane = function(){
                var scrollFraction = editor.renderer.scrollTop * 1.0 / (editorHeight() - clientHeight())
                return scrollFraction * ($$("#pane")[0].scrollHeight - clientHeight())
              }

              var scrollEditor = function(){
                var fractionScroll = pane.scrollTop * 1.0 / (pane.scrollHeight - clientHeight())
                return fractionScroll * (editorHeight() - clientHeight())
              }

              editor.renderer.scrollBar.on("scroll", function(e){
                if(Math.abs(scrollPane() - $$("#pane")[0].scrollTop) > 2){
                  $$("#pane")[0].scrollTop = scrollPane()
                }
              })

              editor.getSession().on('change', function(e){
                ajax.post.data(editor.getValue(), '/ajax/up', '$id', '$x')
              })

            """, x => textInput() = x.convertTo[String])

          ),
          Sig{
            val t = parsed()
            div.fR.w("50%").h("100%")(
              div.w("100%").h("100%").id("pane").css(
                "box-sizing" -> "border-box",
                "overflow-y" -> "scroll"
              ).apply(
                Unparsed(t),
                script(Unparsed(s"""
                $$("#pane").scroll(function(e){
                  if(Math.abs(scrollEditor() - editor.renderer.scrollTop) > 2){
                    editor.renderer.scrollToY(scrollEditor())
                  }
                })

                $$("#pane")[0].scrollTop = scrollPane()

              """))
              )
            )
          }.diff

        )
      )
    )

}
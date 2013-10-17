package html

import java.util.*

  trait Factory<T> {
    fun create() : T
  }

  trait Element

  class TextElement(val text : String) : Element

  abstract class Tag(val name : String) : Element {
    val children = ArrayList<Element>()
    val attributes = HashMap<String, String>()

    protected fun initTag<T : Element>(init :  T.() -> Unit) : T
      where <error>class object T : Factory<T></error>{
      val tag = T.create()
      tag.init()
      children.add(tag)
      return tag
    }
  }

  abstract class TagWithText(name : String) : Tag(name) {
    fun String.plus() {
      children.add(TextElement(this))
    }
  }

  class HTML() : TagWithText("html") {
    class object : Factory<HTML> {
      override fun create() = HTML()
    }

    fun head(init :  Head.() -> Unit) = initTag<Head>(init)

    fun body(init :  Body.() -> Unit) = initTag<Body>(init)
  }

  class Head() : TagWithText("head") {
    class object : Factory<Head> {
      override fun create() = Head()
    }

    fun title(init :  Title.() -> Unit) = initTag<Title>(init)
  }

  class Title() : TagWithText("title")

  abstract class BodyTag(name : String) : TagWithText(name) {
  }

  class Body() : BodyTag("body") {
    class object : Factory<Body> {
      override fun create() = Body()
    }

    fun b(init :  B.() -> Unit) = initTag<B>(init)
    fun p(init :  P.() -> Unit) = initTag<P>(init)
    fun h1(init :  H1.() -> Unit) = initTag<H1>(init)
    fun a(href : String, init :  A.() -> Unit) {
      val a = initTag<A>(init)
      a.href = href
    }
  }

  class B() : BodyTag("b")
  class P() : BodyTag("p")
  class H1() : BodyTag("h1")
  class A() : BodyTag("a") {
    var href : String?
      get() = attributes["href"]
      set(value) {
        if (value != null)
        attributes["href"] = value
      }
  }

  fun MutableMap<String, String>.set(key : String, value : String) = this.put(key, value)

  fun html(init :  HTML.() -> Unit) : HTML {
    val html = HTML()
    html.init()
    return html
  }

fun result(args : Array<String>) =
  html {
    head {
      title {+"XML encoding with Groovy"}
    }
    body {
      h1 {+"XML encoding with Groovy"}
      p {+"this format can be used as an alternative markup to XML"}

      // an element with attributes and text content
      a(href = "http://groovy.codehaus.org") {+"Groovy"}

      // mixed content
      p {
        +"This is some"
        b {+"mixed"}
        +"text. For more see the"
        a(href = "http://groovy.codehaus.org") {+"Groovy"}
        +"project"
      }
      p {+"some text"}

      // content generated by
      p {
        for (arg in args)
          +arg
      }
    }
  }

package pl.immutables.siren

import org.specs2._
import com.yetu.siren.model._
import pl.immutables.siren.generic._

class SirenScalaShapelessSpec extends mutable.Specification {
  case class Foo(name: String, ints: List[BigDecimal])
  case class Bar(id: Option[String], foo: Foo)

  val foo = Foo("foo", List(123.0, 643.0))
  val bar = Bar(Some("bar"), foo)

  "ValueEncoder must" >> {
    "encode classes as siren properties" >> {
      val props = ValueEncoder[Bar].encode(bar).asProps
      val numbersProps = foo.ints.map(v => Property.NumberValue(v))
      val fooProps = Seq(
        ("name", Property.StringValue("foo")),
        ("ints", Property.JsArrayValue(numbersProps))
      )
      props.head === Property("id", Property.StringValue("bar"))
      props.tail.head === Property("foo", Property.JsObjectValue(fooProps))
    }
  }
}

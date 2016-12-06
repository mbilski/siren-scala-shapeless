package pl.immutables.siren

import org.specs2._
import com.yetu.siren.model._
import pl.immutables.siren.encoder._
import pl.immutables.siren.decoder._

class SirenScalaShapelessSpec extends mutable.Specification {
  case class Foo(name: String, ints: List[Int])
  case class Bar(id: Option[String], foo: Foo)

  val foo = Foo("foo", List(123, 643))
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

  "ValueDecoder must" >> {
    "decode siren properties to classes" >> {
      val props = ValueEncoder[Bar].encode(bar)
      val decoded = ValueDecoder[Bar].decode(props)
      decoded === Right(bar)
    }

    "fail on missing property" >> {
      val props = Property.JsObjectValue(List(("name", Property.StringValue("foo"))))
      val decoded = ValueDecoder[Foo].decode(props)
      decoded must beLeft
    }

    "fail on invalid property type" >> {
      val props = Property.JsObjectValue(List(
        ("name", Property.NumberValue(1)),
        ("ints", Property.JsArrayValue(List()))
      ))
      val decoded = ValueDecoder[Foo].decode(props)
      decoded must beLeft
    }
  }
}

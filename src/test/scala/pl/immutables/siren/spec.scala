package pl.immutables.siren

import org.specs2._
import com.yetu.siren.model._
import pl.immutables.siren._
import pl.immutables.siren.encoder._
import pl.immutables.siren.decoder._

class SirenScalaShapelessSpec extends mutable.Specification {
  case class Foo(name: String, ints: Seq[Int])
  case class Bar(id: Option[String], foo: Foo)

  val foo = Foo("foo", List(123, 643))
  val bar = Bar(Some("bar"), foo)

  sealed trait Kind
  case class KindA(a: String) extends Kind
  case class KindB(b: Int) extends Kind

  val ka: Kind = KindA("a")
  val kb: Kind = KindB(2)

  "ValueEncoder must" >> {
    "encode products" >> {
      val props = ValueEncoder[Bar].encode(bar).asProps
      val numbersProps = foo.ints.map(v => Property.NumberValue(v))
      val fooProps = Seq(
        ("name", Property.StringValue("foo")),
        ("ints", Property.JsArrayValue(numbersProps))
      )
      props.head === Property("id", Property.StringValue("bar"))
      props.tail.head === Property("foo", Property.JsObjectValue(fooProps))
    }

    "encode coproducts" >> {
      val enc = ValueEncoder[Kind]
      val pa = enc.encode(ka).asProps
      val pb = enc.encode(kb).asProps
      pa.head === Property(discriminator, Property.StringValue("KindA"))
      pb.head === Property(discriminator, Property.StringValue("KindB"))
      pa.tail.head === Property("a", Property.StringValue("a"))
      pb.tail.head === Property("b", Property.NumberValue(2))
    }
  }

  "ValueDecoder must" >> {
    "decode products" >> {
      val props = ValueEncoder[Bar].encode(bar)
      val decoded = ValueDecoder[Bar].decode(props)
      decoded === Right(bar)
    }

    "decode coproducts" >> {
      val (enc, dcr) = (ValueEncoder[Kind], ValueDecoder[Kind])
      val (pa, pb) = (enc.encode(ka), enc.encode(kb))
      val (da, db) = (dcr.decode(pa), dcr.decode(pb))
      da === Right(ka)
      db === Right(kb)
    }

    "decode optional values" >> {
      val barWithNone = bar.copy(id = None)
      val props = ValueEncoder[Bar].encode(barWithNone)
      val decoded = ValueDecoder[Bar].decode(props)
      decoded === Right(barWithNone)
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

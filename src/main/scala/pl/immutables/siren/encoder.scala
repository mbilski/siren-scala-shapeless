package pl.immutables.siren

import shapeless._
import shapeless.labelled._
import com.yetu.siren.model._

package object encoder {
  trait ValueEncoder[A] {
    def encode(value: A): Property.Value
  }

  object ValueEncoder extends encoders1 {
    implicit val stringEncoder: ValueEncoder[String] = encoder(s => Property.StringValue(s))
    implicit val charEncoder: ValueEncoder[Char] = encoder(s => Property.StringValue(s.toString))
    implicit val intEncoder: ValueEncoder[Int] = encoder(s => Property.NumberValue(s))
    implicit val longEncoder: ValueEncoder[Long] = encoder(s => Property.NumberValue(s))
    implicit val doubleEncoder: ValueEncoder[Double] = encoder(s => Property.NumberValue(s))
    implicit val floatEncoder: ValueEncoder[Float] = encoder(s => Property.NumberValue(s))
    implicit val booleanEncoder: ValueEncoder[Boolean] = encoder(s => Property.BooleanValue(s))
    implicit val bigDecimalEncoder: ValueEncoder[BigDecimal] = encoder(s => Property.NumberValue(s))

    implicit def seqEncoder[A](implicit enc: ValueEncoder[A]): ValueEncoder[Seq[A]] =
        encoder(ls => Property.JsArrayValue(ls.map(enc.encode)))

    implicit def listEncoder[A](implicit enc: ValueEncoder[A]): ValueEncoder[List[A]] =
        encoder(ls => Property.JsArrayValue(ls.map(enc.encode)))

    implicit def optionEncoder[A](implicit enc: ValueEncoder[A]): ValueEncoder[Option[A]] =
        encoder(opt => opt.map(enc.encode).getOrElse(Property.NullValue))

    def apply[A](implicit enc: ValueEncoder[A]): ValueEncoder[A] = enc
  }

  def encoder[A](f: A => Property.Value): ValueEncoder[A] = new ValueEncoder[A] {
    def encode(value: A): Property.Value = f(value)
  }

  trait JsObjectEncoder[A] extends ValueEncoder[A] {
    def encode(value: A): Property.JsObjectValue
  }

  def objectEncoder[A](f: A => Property.JsObjectValue): JsObjectEncoder[A] = new JsObjectEncoder[A] {
    def encode(value: A): Property.JsObjectValue = f(value)
  }

  implicit class ValueProp(v: Property.Value) {
    def asProps: Properties = v match {
      case js: Property.JsObjectValue =>
        js.value.toList.map(v => Property(v._1, v._2))
      case v => sys.error(s"Value $v is not a JsObject")
    }
  }

  trait encoders0 {
    implicit val hNilJsObjectEncoder: JsObjectEncoder[HNil] = objectEncoder(hnil => Property.JsObjectValue(Nil))

    implicit val cNilJsObjectEncoder: JsObjectEncoder[CNil] = objectEncoder(cnil => ???)

    implicit def hListJsObjectEncoder[K <: Symbol, H, T <: HList](implicit
      witness: Witness.Aux[K],
      hEncoder: Lazy[ValueEncoder[H]],
      tEncoder: JsObjectEncoder[T]
    ): JsObjectEncoder[FieldType[K, H] :: T] = {
      val name = witness.value.name
      objectEncoder { hlist =>
        val head = hEncoder.value.encode(hlist.head)
        val tail = tEncoder.encode(hlist.tail)
        Property.JsObjectValue((name, head) +: tail.value)
      }
    }

    implicit def coproductEncoder[K <: Symbol, H, T <: Coproduct](implicit
      witness: Witness.Aux[K],
      hEncoder: Lazy[JsObjectEncoder[H]],
      tEncoder: JsObjectEncoder[T]
    ): JsObjectEncoder[FieldType[K, H] :+: T] = objectEncoder {
      case Inl(h) =>
        val obj = hEncoder.value.encode(h)
        val prop = Property.StringValue(witness.value.name)
        Property.JsObjectValue((discriminator, prop) +: obj.value)
      case Inr(t) =>
        tEncoder.encode(t)
    }
  }

  trait encoders1 extends encoders0 {
    implicit def genericJsObjectEncoder[A, H](implicit
      generic: LabelledGeneric.Aux[A, H],
      hEncoder: Lazy[JsObjectEncoder[H]]
    ): JsObjectEncoder[A] = objectEncoder { value =>
      hEncoder.value.encode(generic.to(value))
    }
  }
}

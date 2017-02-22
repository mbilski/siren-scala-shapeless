package pl.immutables.siren

import shapeless._
import shapeless.labelled._
import com.yetu.siren.model._

package object decoder {
  sealed abstract class DecoderFailure(val message: String) {
    override def toString = s"DecoderFailure($message)"
  }

  final object DecoderFailure {
    def apply(message: String): DecoderFailure = new DecoderFailure(message) {}

    def invalidType(clazz: String, value: Property.Value) =
      DecoderFailure(s"$value's type is not $clazz")

    def missingField(field: String, value: Property.JsObjectValue) =
      DecoderFailure(s"$field is missing in $value")
  }

  trait ValueDecoder[A] {
    def decode(value: Property.Value): Either[DecoderFailure, A]
  }

  object ValueDecoder extends decoders0 {
     implicit val stringDecoder: ValueDecoder[String] = decoder {
      case Property.StringValue(s) => Right(s)
      case prop => Left(DecoderFailure.invalidType("String", prop))
    }

    implicit val intDecoder: ValueDecoder[Int] = decoder {
      case Property.NumberValue(n) => Right(n.toInt)
      case prop => Left(DecoderFailure.invalidType("Int", prop))
    }

    implicit val longDecoder: ValueDecoder[Long] = decoder {
      case Property.NumberValue(n) => Right(n.toLong)
      case prop => Left(DecoderFailure.invalidType("Long", prop))
    }

    implicit val charDecoder: ValueDecoder[Char] = decoder {
      case Property.StringValue(n) if !n.isEmpty => Right(n.charAt(0))
      case prop => Left(DecoderFailure.invalidType("Char", prop))
    }

    implicit val doubleDecoder: ValueDecoder[Double] = decoder {
      case Property.NumberValue(n) => Right(n.toDouble)
      case prop => Left(DecoderFailure.invalidType("Double", prop))
    }

    implicit val floatDecoder: ValueDecoder[Float] = decoder {
      case Property.NumberValue(n) => Right(n.toFloat)
      case prop => Left(DecoderFailure.invalidType("Float", prop))
    }

    implicit val booleanDecoder: ValueDecoder[Boolean] = decoder {
      case Property.BooleanValue(b) => Right(b)
      case prop => Left(DecoderFailure.invalidType("Boolean", prop))
    }

    implicit val bigDecimalDecoder: ValueDecoder[BigDecimal] = decoder {
      case Property.NumberValue(n) => Right(n)
      case prop => Left(DecoderFailure.invalidType("BigDecimal", prop))
    }

    implicit def listDecoder[A](implicit dcr: ValueDecoder[A]): ValueDecoder[List[A]] = decoder {
      case Property.JsArrayValue(ls) =>
        ls.map(dcr.decode).toList.partition(_.isLeft) match {
          case (Nil, vs) => Right(for(Right(i) <- vs) yield i)
          case (errors, _) => Left(errors.head.left.get)
        }
        case prop => Left(DecoderFailure.invalidType("List", prop))
    }

    implicit def seqDecoder[A](implicit dcr: ValueDecoder[A]): ValueDecoder[Seq[A]] = decoder {
      case Property.JsArrayValue(ls) =>
        ls.map(dcr.decode).toList.partition(_.isLeft) match {
          case (Nil, vs) => Right(for(Right(i) <- vs) yield i)
          case (errors, _) => Left(errors.head.left.get)
        }
        case prop => Left(DecoderFailure.invalidType("Seq", prop))
    }

    implicit def optionDecoder[A](implicit dcr: ValueDecoder[A]): ValueDecoder[Option[A]] = decoder {
      case Property.NullValue => Right(None)
      case prop => dcr.decode(prop).right.map(Some(_))
    }

    def apply[A](implicit dcr: ValueDecoder[A]): ValueDecoder[A] = dcr
  }

  def decoder[A](f: Property.Value => Either[DecoderFailure, A]): ValueDecoder[A] = new ValueDecoder[A] {
    def decode(value: Property.Value): Either[DecoderFailure, A] = f(value)
  }

  trait decoders0 {
    implicit val hNilValueDecoder: ValueDecoder[HNil] = decoder(hnil => Right(HNil))

    implicit val cNilValueDecoder: ValueDecoder[CNil] = decoder(cnil =>
        Left(DecoderFailure("Coproduct value not found"))
    )

    implicit def hListValueDecoder[K <: Symbol, H, T <: HList](implicit
        witness: Witness.Aux[K],
        hDecoder: Lazy[ValueDecoder[H]],
        tDecoder: ValueDecoder[T]
    ): ValueDecoder[FieldType[K, H] :: T] = {
        val name = witness.value.name
        decoder {
        case prop @ Property.JsObjectValue(vs) =>
            for {
            v <- vs.find(_._1 == name).toRight(DecoderFailure.missingField(name, prop)).right
            h <- hDecoder.value.decode(v._2).right
            o <- Right(Property.JsObjectValue(vs.filter(_ != v))).right
            t <- tDecoder.decode(o).right
            } yield field[K](h) :: t
        case prop => Left(DecoderFailure.invalidType("Object", prop))
        }
    }

    implicit def coproductDecoder[K <: Symbol, H, T <: Coproduct](implicit
        witness: Witness.Aux[K],
        hDecoder: Lazy[ValueDecoder[H]],
        tDecoder: ValueDecoder[T]
    ): ValueDecoder[FieldType[K, H] :+: T] = {
        val key = (discriminator, Property.StringValue(witness.value.name))
        decoder {
        case prop @ Property.JsObjectValue(vs) =>
            vs.find(_ == key) match {
            case Some(v) => hDecoder.value.decode(prop)
                .right.map(h => Inl(field[K](h)))
            case None => tDecoder.decode(prop)
                .right.map(t => Inr(t))
            }
        case prop => Left(DecoderFailure.invalidType("Coproduct", prop))
        }
    }

    implicit def genericValueDecoder[A, H](implicit
        generic: LabelledGeneric.Aux[A, H],
        hDecoder: Lazy[ValueDecoder[H]]
    ): ValueDecoder[A] = decoder {
        case js: Property.JsObjectValue =>
        for {
            hlist <- hDecoder.value.decode(js).right
        } yield generic.from(hlist)
        case prop => Left(DecoderFailure.invalidType("Object", prop))
    }

    implicit class ObjectProp(ps: Properties) {
        def asJsObject: Property.JsObjectValue =
        Property.JsObjectValue(ps.map(p => (p.name, p.value)))
    }
  }
}

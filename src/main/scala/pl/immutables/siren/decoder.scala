package pl.immutables.siren

import shapeless._
import shapeless.labelled._
import com.yetu.siren.model._

package object decoder {
  trait ValueDecoder[A] {
    def decode(value: Property.Value): Option[A]
  }

  object ValueDecoder {
    def apply[A](implicit dcr: ValueDecoder[A]): ValueDecoder[A] = dcr
  }

  def decoder[A](f: Property.Value => Option[A]): ValueDecoder[A] = new ValueDecoder[A] {
    def decode(value: Property.Value): Option[A] = f(value)
  }

  implicit val stringDecoder: ValueDecoder[String] = decoder {
    case Property.StringValue(s) => Some(s)
    case _ => None
  }

  implicit val intDecoder: ValueDecoder[Int] = decoder {
    case Property.NumberValue(n) => Some(n.toInt)
    case _ => None
  }

  implicit def listDecoder[A](implicit dcr: ValueDecoder[A]): ValueDecoder[List[A]] = decoder {
    case Property.JsArrayValue(ls) =>
      val ols = ls.map(dcr.decode).toList
      if (ols contains None) None else Some(ols.map(_.get))
    case _ => None
  }

  implicit def optionDecoder[A](implicit dcr: ValueDecoder[A]): ValueDecoder[Option[A]] = decoder {
    v => dcr.decode(v).map(Some(_))
  }

  implicit val hNilValueDecoder: ValueDecoder[HNil] = decoder(hnil => Some(HNil))

  implicit def hListValueDecoder[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    hDecoder: Lazy[ValueDecoder[H]],
    tDecoder: ValueDecoder[T]
  ): ValueDecoder[FieldType[K, H] :: T] = {
    val name = witness.value.name
    decoder {
      case Property.JsObjectValue(vs) =>
        for {
          v <- vs.find(_._1 == name)
          h <- hDecoder.value.decode(v._2)
          obj = Property.JsObjectValue(vs.filter(_ != v))
          t <- tDecoder.decode(obj)
        } yield field[K](h) :: t
      case _ => None
    }
  }

  implicit def genericValueDecoder[A, H <: HList](implicit
    generic: LabelledGeneric.Aux[A, H],
    hDecoder: Lazy[ValueDecoder[H]]
  ): ValueDecoder[A] = decoder {
    case js: Property.JsObjectValue =>
      for {
        hlist <- hDecoder.value.decode(js)
      } yield generic.from(hlist)
    case _ => None
  }
}

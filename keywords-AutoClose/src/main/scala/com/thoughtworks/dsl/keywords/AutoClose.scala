package com.thoughtworks.dsl.keywords

import com.thoughtworks.dsl.Dsl
import com.thoughtworks.dsl.Dsl.{!!, Keyword}

import scala.language.implicitConversions

/**
  * @author 杨博 (Yang Bo)
  */
final case class AutoClose[R <: AutoCloseable](open: () => R) extends AnyVal with Keyword[AutoClose[R], R]

object AutoClose {

  implicit def implicitAutoClose[R <: AutoCloseable](r: => R): AutoClose[R] = AutoClose[R](r _)

  def apply[R <: AutoCloseable](r: => R)(
      implicit dummyImplicit: DummyImplicit = DummyImplicit.dummyImplicit): AutoClose[R] = new AutoClose(r _)

  implicit def autoCloseDsl[R <: AutoCloseable]: Dsl[AutoClose[R], AutoCloseable, R] =
    new Dsl[AutoClose[R], AutoCloseable, R] {
      def interpret(autoClose: AutoClose[R], handler: R => AutoCloseable): AutoCloseable = {
        new AutoCloseable {
          private val head = autoClose.open()
          private val tail = handler(head)
          def close(): Unit = {
            try {
              tail.close()
            } finally {
              head.close()
            }
          }
        }
      }
    }

  implicit def streamAutoCloseDsl[R <: AutoCloseable]: Dsl[AutoClose[R], Stream[AutoCloseable], R] =
    new Dsl[AutoClose[R], Stream[AutoCloseable], R] {
      def interpret(keyword: AutoClose[R], handler: R => Stream[AutoCloseable]): Stream[AutoCloseable] = {
        val r = keyword.open()
        val stream = handler(r)
        val otherAutoCloseableInCurrentScope = stream.head

        // Should use `updated`, once https://github.com/scala/bug/issues/10791 is resolved
        new Stream.Cons(new AutoCloseable {
          def close(): Unit = {
            try {
              otherAutoCloseableInCurrentScope.close()
            } finally {
              r.close()
            }
          }
        }, stream.tail)
      }
    }

}

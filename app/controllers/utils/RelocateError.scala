package controllers.utils

import play.api.data.Form

object RelocateError {
  def relocateError[T](form: Form[T], fromKey: String, toKey: String): Form[T] = {
    form.copy(errors = form.errors.map {
      case err if err.key == fromKey => err.copy(key = toKey)
      case err                       => err
    })
  }
}

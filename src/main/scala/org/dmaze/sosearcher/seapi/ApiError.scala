package org.dmaze.sosearcher.seapi

/**
  * Generic API-level exception from the SE API.
  *
  * This will occur if the HTTP request executed successfully, but
  * the [[ApiWrapper]] included error metadata.
  */
class ApiError(message: String) extends Exception(message) {
  def this(message: String, cause: Throwable) {
    this(message)
    initCause(cause)
  }

  def this(cause: Throwable) {
    this(Option(cause).map(_.toString).orNull, cause)
  }

  def this() {
    this(null: String)
  }

  def this(message: String, id: Int, name: String) {
    this(s"${id} ${name}: ${message}")
  }
}

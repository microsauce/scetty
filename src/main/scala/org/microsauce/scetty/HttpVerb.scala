package org.microsauce.scetty

sealed trait HttpVerb {
  val name: String
}

case object GET extends HttpVerb {
  val name = "GET"
}

case object POST extends HttpVerb {
  val name = "POST"
}

case object PUT extends HttpVerb {
  val name = "PUT"
}

case object DELETE extends HttpVerb {
  val name = "DELETE"
}

case object USE extends HttpVerb {
  val name = "USE"
}

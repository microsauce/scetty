package org.microsauce.scetty

sealed trait HttpVerb {val verb:String}
case object GET extends HttpVerb {val verb = "get"}
case object POST extends HttpVerb {val verb = "post"}
case object PUT extends HttpVerb {val verb = "put"}
case object DELETE extends HttpVerb {val verb = "delete"}
case object USE extends HttpVerb {val verb = "use"}
case object WS extends HttpVerb {val verb = "ws"}

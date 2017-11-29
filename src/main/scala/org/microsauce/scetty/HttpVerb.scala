package org.microsauce.scetty

// TODO doc

sealed trait HttpVerb {val name:String}

case object GET extends HttpVerb {val name = "GET"}
case object POST extends HttpVerb {val name = "POST"}
case object PUT extends HttpVerb {val name = "PUT"}
case object DELETE extends HttpVerb {val name = "DELETE"}
case object USE extends HttpVerb {val name = "USE"}
case object WS extends HttpVerb {val name = "WS"}

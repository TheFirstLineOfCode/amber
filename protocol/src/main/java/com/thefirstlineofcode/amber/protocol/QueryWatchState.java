package com.thefirstlineofcode.amber.protocol;

import com.thefirstlineofcode.basalt.oxm.coc.annotations.ProtocolObject;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;

@ProtocolObject(namespace="urn:leps:things:amber", localName="query-watch-state")
public class QueryWatchState {
	public static final Protocol PROTOCOL = new Protocol("urn:leps:things:amber", "query-watch-state");
}

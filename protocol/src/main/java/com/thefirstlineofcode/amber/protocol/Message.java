package com.thefirstlineofcode.amber.protocol;

import com.thefirstlineofcode.basalt.oxm.coc.annotations.ProtocolObject;
import com.thefirstlineofcode.basalt.xmpp.core.Protocol;

@ProtocolObject(namespace="urn:leps:things:amber", localName="message")
public class Message {
	public static final Protocol PROTOCOL = new Protocol("urn:leps:things:amber", "message");
	
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}

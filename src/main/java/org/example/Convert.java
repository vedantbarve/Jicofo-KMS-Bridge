package org.example;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jitsi.xmpp.extensions.jingle.JingleIQ;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Convert {
    public void Jingle2SDP(JingleIQ jingleIQ, boolean removeTcpCandidates, boolean failICE) throws IOException, JDOMException {

        //xml parse
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new StringReader(jingleIQ.toXML().toString()));


        Element classElement = doc.getRootElement();

        // jingle tag attributes as variable
        Element jingle = classElement.getChildren().getFirst();


        //sdp holding variable
        StringBuilder sdpBuilder = new StringBuilder();

        // Add first four default values
        sdpBuilder.append("v=0\n");
        sdpBuilder.append(STR."o=- \{jingleIQ.getSID()} 2 IN IP4 0.0.0.0\n");
        sdpBuilder.append("s=-\n");
        sdpBuilder.append("t=0 0\n");

        List<Element> jingleChildren = jingle.getChildren();

        // Add a=group:bundle
        StringBuilder bundle = new StringBuilder();
        bundle.append("a=group:BUNDLE ");


        var groups = jingleChildren.stream().filter(element -> element.getName().equals("group"));
        groups.forEach(
                group->{
                    for(var child : group.getChildren()){
                        if(!child.getAttributeValue("name").equals("data")){
                            bundle.append(STR."\{child.getAttributeValue("name")} ");
                        }
                    }
                }
        );
        sdpBuilder.append(STR."\{bundle}\n");

        var contents = jingleChildren.stream().filter(element -> element.getName().equals("content"));
        for(var content : jingleChildren){
            if(content.getName().equals("content") && !content.getAttributeValue("name").equals("data")){
                List<Element> contentChildren = content.getChildren();

                Element desc = null;
                Element transport = null;
                List<Element> transportChildren = null;
                List<Element> descChildren = null;

                for (Element contentChild : contentChildren){
                    if (desc == null) {
                        desc = (contentChild.getName().equals("description")) ? contentChild : null;
                    }
                    if (transport == null) {
                        transport = (contentChild.getName().equals("transport")) ? contentChild : null;
                    }
                }

                if (transport != null) transportChildren = transport.getChildren();
                if (desc != null) descChildren = desc.getChildren();

                String mediaPort = "9";
                if(content.getAttributeValue("senders").equals("rejected")) mediaPort = "0";

                Element sctp = null;
                Element fingerprint = null;

                if(transport != null && !transportChildren.isEmpty()){
                    for (Element data : transport.getChildren()) {
                        if (data.getName().equals("fingerprint") ) {
                            fingerprint = data;
                        }
                        if (data.getName().equals("sctp") && data.getAttributeValue("xmlns").equals("urn:xmpp:jingle:transports:dtls-sctp:1")) {
                            sctp = data;
                        }
                    }
                }

                StringBuilder proto = new StringBuilder();
                if (fingerprint != null) {
                    proto.append((sctp == null) ? "UDP/DTLS/SCTP" : "UDP/TLS/RTP/SAVPF");
                } else {
                    proto.append("UDP/TLS/RTP/SAVPF");
                }

                if (sctp != null) {
                    sdpBuilder.append(STR."m=application \{mediaPort} UDP/DTLS/SCTP webrtc-datachannel\n");
                    sdpBuilder.append(STR."a=sctp-port:\{sctp.getAttributeValue("number")}\n");
                    sdpBuilder.append("a=max-message-size:262144\n");
                } else {
                    // handle via SDPUtils
                    StringBuilder fmt = new StringBuilder();
                    if (descChildren != null) {
                        var data = descChildren.stream().filter(element -> element.getName().equals("payload-type"));
                        data.forEach(value->fmt.append(STR."\{value.getAttributeValue("id")} "));
                    }
                    if (desc != null) {
                        String media = desc.getAttributeValue("media");
                        sdpBuilder.append(STR."m=\{media} \{mediaPort} \{proto} \{fmt}\n");
                    }

                }

                sdpBuilder.append("c=IN IP4 0.0.0.0\n");

                if (sctp == null) {
                    sdpBuilder.append("a=rtcp:1 IN IP4 0.0.0.0\n");
                }


                // process transport tag and corresponding children tags
                if(transport != null){
                    String ufrag = transport.getAttributeValue("ufrag");
                    if (ufrag != null) {
                        sdpBuilder.append(STR."a=ice-ufrag:\{ufrag}\n");
                    }
                    String pwd = transport.getAttributeValue("pwd");
                    if (pwd != null) {
                        sdpBuilder.append(STR."a=ice-pwd:\{pwd}\n");
                    }

                    if(!transportChildren.isEmpty()){
                        var fingerprintTag = transportChildren.stream().filter(element->element.getName().equals("fingerprint"));
                        fingerprintTag.forEach(
                                data -> {
                                    String hash = data.getAttributeValue("hash");
                                    String value = data.getValue();
                                    sdpBuilder.append(STR."a=fingerprint:\{hash} \{value}\n");
                                    String setup = data.getAttributeValue("setup");
                                    if (setup != null) {
                                        sdpBuilder.append(STR."a=setup:\{setup}\n");
                                    }
                                }
                        );
                        var candidateTag = transportChildren.stream().filter(element->element.getName().equals("candidate"));
                        candidateTag.forEach(
                                data -> {
                                    String protocol = data.getAttributeValue("protocol");
                                    // removeTcpCandidates section from transport.find line 669

                                    if(removeTcpCandidates && (protocol.equals("tcp") || protocol.equals("udp"))){
                                        return;
                                    }if(failICE){
                                        data.setAttribute("ip","1.1.1.1");
                                    }

                                    String foundation = data.getAttributeValue("foundation");
                                    String component = data.getAttributeValue("component");
                                    String priority = data.getAttributeValue("priority");
                                    String ip = data.getAttributeValue("ip");
                                    String port = data.getAttributeValue("port");
                                    String type = data.getAttributeValue("type");
                                    String relAddr = data.getAttributeValue("rel-addr");
                                    String relPort = data.getAttributeValue("rel-port");

                                    sdpBuilder.append(STR."a=candidate:\{foundation} \{component} \{protocol} \{priority} \{ip} \{port} typ ");
                                    switch (type) {
                                        case "srflx":
                                        case "prflx":
                                        case "relay":
                                            if (relAddr != null && relPort != null) {
                                                sdpBuilder.append(STR."raddr \{relAddr} rport \{relPort} ");
                                            }
                                    }

                                    if (protocol.equals("tcp")) {
                                        String tcpType = data.getAttributeValue("rel-port");
                                        sdpBuilder.append(STR."tcptype \{tcpType} ");
                                    }
                                    String generation = data.getAttributeValue("generation");
                                    if (generation != null) {
                                        sdpBuilder.append(STR."generation \{generation}\n");
                                    } else {
                                        sdpBuilder.append("generation 0\n");
                                    }
                                }
                        );
                    }
                }

                String senders = content.getAttributeValue("senders");
                switch (senders) {
                    case "initiator":
                        sdpBuilder.append("a=sendonly\n");
                        break;
                    case "responder":
                        sdpBuilder.append("a=recvonly\n");
                        break;
                    case "none":
                        sdpBuilder.append("a=inactive\n");
                        break;
                    case "both":
                        sdpBuilder.append("a=sendrecv\n");
                        break;
                }

                String name = content.getAttributeValue("name");
                sdpBuilder.append(STR."a=mid:\{name}\n");

                if(desc != null && descChildren != null && !descChildren.isEmpty()){
                    var rtcpmuxTag = descChildren.stream().filter(element -> element.getName().equals("rtcp-mux"));
                    if(!rtcpmuxTag.toList().isEmpty()){
                        sdpBuilder.append("rtcp-mux\n");
                    }

                    var payloadTypeTag = descChildren.stream().filter(element -> element.getName().equals("payload-type"));
                    payloadTypeTag.forEach(
                            data -> {
                                String payloadID = data.getAttributeValue("id");
                                String payloadName = data.getAttributeValue("name");
                                String clockrate = data.getAttributeValue("clockrate");
                                sdpBuilder.append(STR."a=rtpmap:\{payloadID} \{payloadName}/\{clockrate}");
                                String channels = data.getAttributeValue("channels");
                                if (channels != null && !channels.equals("1")) {
                                    sdpBuilder.append(STR."/\{channels}\n");
                                } else {
                                    sdpBuilder.append("\n");
                                }
                                sdpBuilder.append(STR."a=fmtp:\{payloadID} ");

                                var parameterTag = data.getChildren().stream().filter(element -> data.getName().equals("parameter"));
                                parameterTag.forEach(
                                        parameter -> {
                                            String parameterName = parameter.getAttributeValue("name");
                                            String parameterValue = parameter.getAttributeValue("value");
                                            if (parameterName != null && !parameterName.isEmpty()) {
                                                sdpBuilder.append(STR."\{parameterName}=\{parameterValue}");
                                                if (parameter != parameter.getChildren().getLast()) {
                                                    sdpBuilder.append(";");
                                                }
                                            }
                                        }
                                );
                                sdpBuilder.append("\n");

                                var rtcpfbtrrintTag = data.getChildren().stream().filter(element -> element.getName().equals("rtcp-fb-trr-int"));
                                rtcpfbtrrintTag.forEach(
                                        rtcpfbtrrint -> {
                                            sdpBuilder.append("a=rtcp-fb:* trr-int ");
                                            String feedbackEleValue = rtcpfbtrrint.getAttributeValue("value");
                                            if (feedbackEleValue != null) {
                                                sdpBuilder.append(feedbackEleValue);
                                            } else {
                                                sdpBuilder.append("0");
                                            }
                                            sdpBuilder.append("\n");
                                        }
                                );

                                var rtcpfbTag = data.getChildren().stream().filter(element -> element.getName().equals("rtcp-fb"));
                                rtcpfbTag.forEach(
                                        rtcpfb -> {
                                            String fbtype = rtcpfb.getAttributeValue("type");
                                            sdpBuilder.append(STR."a=rtcp-fb:\{payloadID} \{fbtype}");
                                            if (rtcpfb.getAttributeValue("subtype") != null) {
                                                sdpBuilder.append(rtcpfb.getAttributeValue("subtype"));
                                            }
                                            sdpBuilder.append("\n");
                                        }
                                );

                                var rtphdrextTag = data.getChildren().stream().filter(element -> element.getName().equals("rtp-hdrext"));
                                rtphdrextTag.forEach(
                                        rtphdrext -> {
                                            String descID = rtphdrext.getAttributeValue("id");
                                            String descURI = rtphdrext.getAttributeValue("uri");
                                            sdpBuilder.append(STR."a=extmap:\{descID} \{descURI}\n");
                                        }
                                );
                            }
                    );

                    var rtcpfbtrrintTag = desc.getChildren().stream().filter(element -> element.getName().equals("rtcp-fb-trr-int"));
                    rtcpfbtrrintTag.forEach(
                            rtcpfbtrrint -> {
                                sdpBuilder.append("a=rtcp-fb:* trr-int ");
                                String feedbackEleValue = rtcpfbtrrint.getAttributeValue("value");
                                if (feedbackEleValue != null) {
                                    sdpBuilder.append(feedbackEleValue);
                                } else {
                                    sdpBuilder.append("0");
                                }
                                sdpBuilder.append("\n");
                            }
                    );

                    var rtcpfbTag = desc.getChildren().stream().filter(element -> element.getName().equals("rtcp-fb"));
                    rtcpfbTag.forEach(
                            rtcpfb -> {
                                String fbtype = rtcpfb.getAttributeValue("type");
                                sdpBuilder.append(STR."a=rtcp-fb:* \{fbtype}");
                                if (rtcpfb.getAttributeValue("subtype") != null) {
                                    sdpBuilder.append(rtcpfb.getAttributeValue("subtype"));
                                }
                                sdpBuilder.append("\n");
                            }
                    );

                    // TODO: xep-0294
                    var rtphdrextTag = desc.getChildren().stream().filter(element -> element.getName().equals("rtp-hdrext"));
                    rtphdrextTag.forEach(
                            rtphdrext -> {
                                String descID = rtphdrext.getAttributeValue("id");
                                String descURI = rtphdrext.getAttributeValue("uri");
                                sdpBuilder.append(STR."a=extmap:\{descID} \{descURI}\n");
                            }
                    );

                    var extmapallowmixedTag = descChildren.stream().filter(element -> element.getName().equals("extmap-allow-mixed"));
                    if (!extmapallowmixedTag.toList().isEmpty()) sdpBuilder.append("a=extmap-allow-mixed\n");

                    var ssrcgroupTag = descChildren.stream().filter(element -> element.getName().equals("ssrc-group"));
                    ssrcgroupTag.forEach(
                            data -> {
                                StringBuilder sscrsStringBulder = new StringBuilder();
                                String semantics = data.getAttributeValue("semantics");
                                List<Element> sscrs = data.getChildren();
                                for (Element ssrc : sscrs) {
                                    sscrsStringBulder.append(STR."\{ssrc.getAttributeValue("ssrc")} ");
                                }
                                sdpBuilder.append(STR."a=ssrc-group:\{semantics} \{sscrsStringBulder}\n");
                            }
                    );

                    StringBuilder userSources = new StringBuilder();
                    StringBuilder nonUserSources = new StringBuilder();

                    var sourceTag = descChildren.stream().filter(element -> element.getName().equals("source"));
                    sourceTag.forEach(
                            data -> {
                                String ssrc = data.getAttributeValue("ssrc");
                                AtomicBoolean isUserSource = new AtomicBoolean(true);
                                StringBuilder sourceStr = new StringBuilder();
                                var parameterTag = data.getChildren().stream().filter(element -> element.getName().equals("parameter"));
                                parameterTag.forEach(
                                        parameter -> {
                                            String paramName = parameter.getAttributeValue("name");
                                            String paramValue = parameter.getAttributeValue("value");
                                            sourceStr.append(STR."a=ssrc:\{ssrc} \{paramName}");
                                            if (paramValue != null && !paramValue.isEmpty()) {
                                                sourceStr.append(STR.":\{paramValue}");
                                            }
                                            sourceStr.append("\n");
                                            if (paramValue != null && paramValue.contains("mixedmslabel")) {
                                                isUserSource.set(false);
                                            }
                                        }
                                );
                                if (isUserSource.get()) {
                                    userSources.append(sourceStr);
                                } else {
                                    nonUserSources.append(sourceStr);
                                }
                            }
                    );
                    sdpBuilder.append(STR."\{userSources}\{nonUserSources}");
                }
            }
        }
        // Print sdp
        System.out.println(sdpBuilder);
    }

    public void SDP2Jingle(JingleIQ jingleIQ, boolean removeTcpCandidates, boolean failICE){
    }
}

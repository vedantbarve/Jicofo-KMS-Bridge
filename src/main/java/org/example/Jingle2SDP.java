package org.example;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;

public class Jingle2SDP {
    private String xml;
    private String sessionID;
    private boolean removeTcpCandidates;
    private boolean failICE;

    Jingle2SDP(String xml,String sessionID,boolean removeTcpCandidates,boolean failICE){
        this.xml = xml;
        this.sessionID = sessionID;
        this.failICE = failICE;
        this.removeTcpCandidates = removeTcpCandidates;
    }

    public String jingle2SDP() throws IOException, JDOMException {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new StringReader(this.xml));

        Element classElement = doc.getRootElement();
        Element jingle = classElement.getChildren().getFirst();
        List<Element> jingleChildren = jingle.getChildren();

        //sdp holding variable
        StringBuilder sdpBuilder = new StringBuilder();
        // Add first four default values
        sdpBuilder.append("v=0\n");
        sdpBuilder.append(STR."o=- \{sessionID} 2 IN IP4 0.0.0.0\n");
        sdpBuilder.append("s=-\n");
        sdpBuilder.append("t=0 0\n");

        // Add a=group:bundle
        StringBuilder bundle = new StringBuilder();
        bundle.append("a=group:BUNDLE ");
        for (Element val : jingleChildren) {
            if (Objects.equals(val.getName(), "group")) {
                val.getChildren().forEach(
                        child -> bundle.append(STR."\{child.getAttributeValue("name")} ")
                );
            }
        }
        sdpBuilder.append(STR."\{bundle}\n");

        // Add media
        for (Element element : jingleChildren) {
            // Filters content in tag only
            if (Objects.equals(element.getName(), "content")) {
                // Loops through all the content tags in the jingle tag
                Element desc = null;
                Element transport = null;
                List<Element> contentChildren = element.getChildren();
                for (Element contentChild : contentChildren) {
                    // children tags of a content tag
                    if (desc == null) {
                        desc = (Objects.equals(contentChild.getName(), "description")) ? contentChild : null;
                    }
                    if (transport == null) {
                        transport = (Objects.equals(contentChild.getName(), "transport")) ? contentChild : null;
                    }

                }
                List<Element> transportChildren = transport.getChildren();
                List<Element> descChildren = desc.getChildren();
                Element sctp = null;
                Element fingerprint = null;

                // Assign mediaPort as 9
                String mediaPort = "9";

                if (Objects.equals(element.getAttribute("senders").getValue(), "rejected")) {
                    mediaPort = "0";
                }

                for (Element data : transportChildren) {
                    if (data.getName().equals("fingerprint") && data.getNamespace().toString().equals("urn:xmpp:jingle:apps:dtls:0")) {
                        fingerprint = data;
                    }
                    if (data.getName().equals("sctp") && data.getNamespace().toString().equals("urn:xmpp:jingle:transports:dtls-sctp:1")) {
                        sctp = data;
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
                    for (Element value : descChildren) {
                        if (value.getName().equals("payload-type")) {
                            fmt.append(STR."\{value.getAttributeValue("id")} ");
                        }
                    }
                    sdpBuilder.append(STR."m=\{desc.getAttributeValue("media")} \{mediaPort} \{proto} \{fmt}\n");

                }
                sdpBuilder.append("c=IN IP4 0.0.0.0\n");

                if (sctp == null) {
                    sdpBuilder.append("a=rtcp:1 IN IP4 0.0.0.0\n");
                }

                String ufrag = transport.getAttributeValue("ufrag");
                if (ufrag != null) {
                    sdpBuilder.append(STR."a=ice-ufrag:\{ufrag}\n");
                }
                String pwd = transport.getAttributeValue("pwd");
                if (pwd != null) {
                    sdpBuilder.append(STR."a=ice-pwd:\{pwd}\n");
                }

                for (Element transportChild : transport.getChildren()) {
                    String xmlns = "urn:xmpp:jingle:apps:dtls:0";
                    if (Objects.equals(transportChild.getAttributeValue("xmlns"), xmlns)) {
                        String hash = transportChild.getAttributeValue("hash");
                        String fingerprintValue = transportChild.getValue();
                        sdpBuilder.append(STR."a=fingerprint:\{hash} \{fingerprintValue}\n");
                        String setup = transportChild.getAttributeValue("setup");
                        if (setup != null) {
                            sdpBuilder.append(STR."a=setup:\{setup}\n");
                        }
                    }
                    if (transportChild.getName().equals("candidate")) {
                        String protocol = transportChild.getAttributeValue("protocol");
                        // removeTcpCandidates section from transport.find line 669
                        if(failICE){
                            transportChild.setAttribute("ip","1.1.1.1");
                        }

                        String foundation = transportChild.getAttributeValue("foundation");
                        String component = transportChild.getAttributeValue("component");
                        String priority = transportChild.getAttributeValue("priority");
                        String ip = transportChild.getAttributeValue("ip");
                        String port = transportChild.getAttributeValue("port");
                        String type = transportChild.getAttributeValue("type");
                        String relAddr = transportChild.getAttributeValue("rel-addr");
                        String relPort = transportChild.getAttributeValue("rel-port");

                        sdpBuilder.append(STR."a=candidate: \{foundation} \{component} \{protocol} \{priority} \{ip} \{port} typ ");
                        switch (type) {
                            case "srflx":
                                ;
                            case "prflx":
                                ;
                            case "relay":
                                if (relAddr != null && relPort != null) {
                                    sdpBuilder.append(STR."raddr \{relAddr} rport \{relPort} ");
                                }
                        }

                        if (protocol.equals("tcp")) {
                            String tcpType = transportChild.getAttributeValue("rel-port");
                            sdpBuilder.append(STR."tcptype \{tcpType} ");
                        }
                        String generation = transportChild.getAttributeValue("generation");
                        if (generation != null) {
                            sdpBuilder.append(STR."generation \{generation}\n");
                        } else {
                            sdpBuilder.append("generation 0\n");
                        }
                    }

                }

                String senders = element.getAttributeValue("senders");

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
                String name = element.getAttributeValue("name");
                sdpBuilder.append(STR."a=mid:\{name}\n");

                for (Element node : descChildren) {
                    if (node.getName().equals("rtcp-mux")) {
                        sdpBuilder.append("a=rtcp-mux\n");
                    }
                }
                for (Element node : descChildren) {
                    if (node.getName().equals("payload-type")) {
                        String payloadID = node.getAttributeValue("id");
                        String payloadName = node.getAttributeValue("name");
                        String clockrate = node.getAttributeValue("clockrate");
                        sdpBuilder.append(STR."a=rtpmap:\{payloadID} \{payloadName}/\{clockrate}");
                        String channels = node.getAttributeValue("channels");
                        if (channels != null && !channels.equals("1")) {
                            sdpBuilder.append(STR."/\{channels}\n");
                        } else {
                            sdpBuilder.append("\n");
                        }

                        List<Element> payloadChildren = node.getChildren();
                        sdpBuilder.append(STR."a=fmtp:\{payloadID} ");
                        for (Element payloadChild : payloadChildren) {
                            if (payloadChild.getName().equals("parameter")) {
                                String parameterName = payloadChild.getAttributeValue("name");
                                String parameterValue = payloadChild.getAttributeValue("value");
                                if (parameterName != null && !parameterName.isEmpty()) {
                                    sdpBuilder.append(STR."\{parameterName}=\{parameterValue}");
                                    if (payloadChild != payloadChildren.getLast()) {
                                        sdpBuilder.append(";");
                                    }
                                }
                            }
                        }
                        sdpBuilder.append("\n");

                        // inside payload-type
                        for (Element payloadChild : payloadChildren) {
                            if (payloadChild.getName().equals("rtcp-fb-trr-int")) {
                                sdpBuilder.append("a=rtcp-fb:* trr-int ");
                                String feedbackEleValue = payloadChild.getAttributeValue("value");

                                if (feedbackEleValue != null) {
                                    sdpBuilder.append(feedbackEleValue);
                                } else {
                                    sdpBuilder.append("0");
                                }
                                sdpBuilder.append("\n");
                            }
                            if (payloadChild.getName().equals("rtcp-fb")) {
                                String fbtype = payloadChild.getAttributeValue("type");
                                sdpBuilder.append(STR."a=rtcp-fb:\{payloadID} \{fbtype}");
                                if (payloadChild.getAttributeValue("subtype") != null) {
                                    sdpBuilder.append(payloadChild.getAttributeValue("subtype"));
                                }
                                sdpBuilder.append("\n");
                            }
                        }

                        // XEP-2093 desc
                        for (Element descChild : descChildren) {
                            if (descChild.getName().equals("rtcp-fb-trr-int")) {
                                sdpBuilder.append("a=rtcp-fb:* trr-int ");
                                String feedbackEleValue = descChild.getAttributeValue("value");

                                if (feedbackEleValue != null) {
                                    sdpBuilder.append(feedbackEleValue);
                                } else {
                                    sdpBuilder.append("0");
                                }
                                sdpBuilder.append("\n");
                            }
                            if (descChild.getName().equals("rtcp-fb")) {
                                String fbtype = descChild.getAttributeValue("type");
                                sdpBuilder.append(STR."a=rtcp-fb:\{payloadID} \{fbtype}");
                                if (descChild.getAttributeValue("subtype") != null) {
                                    sdpBuilder.append(descChild.getAttributeValue("subtype"));
                                }
                                sdpBuilder.append("\n");
                            }
                            if (descChild.getName().equals("rtp-hdrext")) {
                                String descID = descChild.getAttributeValue("id");
                                String descURI = descChild.getAttributeValue("uri");
                                sdpBuilder.append(STR."a=extmap:\{descID} \{descURI}\n");
                            }
                            if (descChild.getName().equals("extmap-allow-mixed")) {
                                sdpBuilder.append("a=extmap-allow-mixed\n");
                            }
                            if (descChild.getName().equals("ssrc-group")) {
                                StringBuilder sscrsStringBulder = new StringBuilder();
                                String semantics = descChild.getAttributeValue("semantics");
                                List<Element> sscrs = descChild.getChildren();
                                for (Element ssrc : sscrs) {
                                    sscrsStringBulder.append(STR."\{ssrc.getAttributeValue("ssrc")} ");
                                }
                                sdpBuilder.append(STR."a=ssrc-group:\{semantics} \{sscrsStringBulder}\n");
                            }
                            StringBuilder userSources = new StringBuilder();
                            StringBuilder nonUserSources = new StringBuilder();
                            if (descChild.getName().equals("source")) {
                                String ssrc = descChild.getAttributeValue("ssrc");
                                boolean isUserSource = true;
                                StringBuilder sourceStr = new StringBuilder();
                                for (Element val : descChild.getChildren()) {
                                    if (val.getName().equals("parameter")) {
                                        String paramName = val.getAttributeValue("name");
                                        String paramValue = val.getAttributeValue("value");
                                        sourceStr.append(STR."a=ssrc:\{ssrc} \{paramName}");
                                        if (paramValue != null && !paramValue.isEmpty()) {
                                            sourceStr.append(STR.":\{paramValue}");
                                        }
                                        sourceStr.append("\n");
                                        if (paramValue != null && paramValue.contains("mixedmslabel")) {
                                            isUserSource = false;
                                        }
                                    }
                                }
                                if (isUserSource) {
                                    userSources.append(sourceStr);
                                } else {
                                    nonUserSources.append(sourceStr);
                                }
                            }

                            sdpBuilder.append(STR."\{userSources}\{nonUserSources}");
                        }

                    }
                }
            }
        }
        return sdpBuilder.toString();
    }
}

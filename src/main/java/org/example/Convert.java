package org.example;

import org.example.dataclasses.ParameterDataClass;
import org.example.dataclasses.RTCPFBDataClass;
import org.example.dataclasses.SourceParameterDataClass;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jitsi.xmpp.extensions.jingle.JingleIQ;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class Convert {

    String session = "";
    String raw = "";
    ArrayList<String> media = new ArrayList<>();

    public String Jingle2SDP(JingleIQ jingleIQ, boolean removeTcpCandidates, boolean failICE) throws IOException, JDOMException {

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
        bundle.append("a=group:BUNDLE");


        var groups = jingleChildren.stream().filter(element -> element.getName().equals("group"));
        groups.forEach(
                group->{
                    for(var child : group.getChildren()){
                        if(!child.getAttributeValue("name").equals("data")){
                            bundle.append(STR." \{child.getAttributeValue("name")}");
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
                    proto.append((sctp != null) ? " UDP/DTLS/SCTP" : " UDP/TLS/RTP/SAVPF");
                } else {
                    proto.append(" UDP/TLS/RTP/SAVPF");
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
                        data.forEach(value->fmt.append(STR." \{value.getAttributeValue("id")}"));
                    }
                    if (desc != null) {
                        String media = desc.getAttributeValue("media");
                        sdpBuilder.append(STR."m=\{media} \{mediaPort}\{proto}\{fmt}\n");
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

                                    sdpBuilder.append(STR."a=candidate:\{foundation} \{component} \{protocol} \{priority} \{ip} \{port} typ \{type}");
                                    switch (type) {
                                        case "srflx":
                                        case "prflx":
                                        case "relay":
                                            if (relAddr != null && relPort != null) {
                                                sdpBuilder.append(STR." raddr \{relAddr} rport \{relPort}");
                                            }
                                    }

                                    if (protocol.equals("tcp")) {
                                        String tcpType = data.getAttributeValue("rel-port");
                                        sdpBuilder.append(STR." tcptype \{tcpType}");
                                    }
                                    String generation = data.getAttributeValue("generation");
                                    if (generation != null) {
                                        sdpBuilder.append(STR." generation \{generation}\n");
                                    } else {
                                        sdpBuilder.append(" generation 0\n");
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
                        sdpBuilder.append("a=rtcp-mux\n");
                    }

                    var payloadTypeTag = descChildren.stream().filter(element -> element.getName().equals("payload-type"));
                    payloadTypeTag.forEach(
                            data -> {
                                String payloadID = data.getAttributeValue("id");
                                String payloadName = data.getAttributeValue("name");
                                String clockrate = data.getAttributeValue("clockrate");

                                String channels = data.getAttributeValue("channels");
                                if(channels == null){
                                    channels = "";
                                }else {
                                    channels = STR."/\{channels}";
                                }

                                sdpBuilder.append(STR."a=rtpmap:\{payloadID} \{payloadName}/\{clockrate}\{channels}\n");

                                var parameterTag = data.getChildren().stream().filter(element -> element.getName().equals("parameter"));
                                var parameters = parameterTag.toList();
                                if(!parameters.isEmpty()){
                                    sdpBuilder.append(STR."a=fmtp:\{payloadID} ");
                                    for(var parameter : parameters){
                                        {
                                            String parameterName = parameter.getAttributeValue("name");
                                            String parameterValue = parameter.getAttributeValue("value");
                                            if (parameterName != null && !parameterName.isEmpty()) sdpBuilder.append(STR."\{parameterName}=");
                                            if (parameterValue != null && !parameterValue.isEmpty()) sdpBuilder.append(STR."\{parameterValue};");
                                        }
                                    }
                                    sdpBuilder.deleteCharAt(sdpBuilder.length()-1);
                                    sdpBuilder.append("\n");
                                }


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
                                                sdpBuilder.append(STR." \{rtcpfb.getAttributeValue("subtype")}");
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
        return sdpBuilder.toString();
    }

    public String SDP2Jingle(String sdp,String sessionID,String initiator, boolean removeTcpCandidates, boolean failICE) throws ParserConfigurationException {
        session = "";
        raw = "";
        media = new ArrayList<>();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        //root element
        org.w3c.dom.Document doc = docBuilder.newDocument();

        // create iq tag aka root
        org.w3c.dom.Element rootElement = doc.createElement("iq");

        // set iq level attributes
//        rootElement.setAttribute("xmlns", "jabber:client");
//        rootElement.setAttribute("to", to);
//        rootElement.setAttribute("from", from);
//        rootElement.setAttribute("id", "SDE45-1");
//        rootElement.setAttribute("type", "set");

        // create jingle tag
        org.w3c.dom.Element jingleElement = doc.createElement("jingle");

        // set jingle level attributes
        jingleElement.setAttribute("xmlns", "urn:xmpp:jingle:1");
        jingleElement.setAttribute("action", "session-initiate");
        jingleElement.setAttribute("initiator", initiator);
        jingleElement.setAttribute("sid", sessionID);

        rootElement.appendChild(jingleElement);

        String[] mediaList = sdp.split("\nm=");
        for (int i = 1; i < mediaList.length; i++) {
            String mediaI = STR."m=\{mediaList[i]}";
            if (i != mediaList.length - 1) {
                mediaI += "\n";
            }
            mediaList[i] = mediaI;
        }

        media = new ArrayList<>(Arrays.asList(mediaList));

        // shift
        session = STR."\{media.getFirst()}\n";
        media.removeFirst();

        // raw = session + media
        raw = sdp;

        // create group tag within jingle tag
        ArrayList<String> data = findLines(session, "a=group:", null);
        for (var value : data) {
            ArrayList<String> parts = new ArrayList<>(Arrays.asList(value.split(" ")));

            // shift
            var semantics = parts.getFirst().substring(8);
            parts.removeFirst();

            org.w3c.dom.Element group = doc.createElement("group");
            group.setAttribute("xmlns", "urn:xmpp:jingle:apps:grouping:0");
            group.setAttribute("semantics", semantics);


            for (String part : parts) {
                org.w3c.dom.Element content = doc.createElement("content");
                content.setAttribute("name", part);
                group.appendChild(content);
            }

            for (int i = 0; i < media.size(); i++) {
                var mline = parseMLine(media.get(i).split("\n")[0]);
                if (!(mline.get("media").equals("audio") || mline.get("media").equals("video"))) {
                    continue;
                }

                String ssrc = "";
                String assrcLine = findLine(media.get(i), "a=ssrc:", null);

                if (assrcLine != null) {
                    ssrc = assrcLine.substring(7).split(" ")[0];
                }

                // add content
                org.w3c.dom.Element content = doc.createElement("content");
                content.setAttribute("creator", "initiator");
                content.setAttribute("name", mline.get("media").toString());

                String amidLine = findLine(media.get(i), "a=mid:", null);

                // redundant
                if (amidLine != null) {
                    String mid = amidLine.substring(6);
                    content.setAttribute("name",mid);
                }
//                unknown parameter initialLastN
//                if(mline.get("media").equals("video") && initialLastN == "number"){
//                    // lib-jitsi-meet line 200
//                }

                // start description tag processing
                if (mline.get("media").equals("audio") || mline.get("media").equals("video")) {
                    // add description
                    org.w3c.dom.Element description = doc.createElement("description");
                    description.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:1");
                    description.setAttribute("media", mline.get("media").toString());
                    if(mline.get("media").toString().equals("audio")) description.setAttribute("maxptime", "60");
//                    if (ssrc != null) {
//                        description.setAttribute("ssrc", ssrc);
//                    }
                    // TODO: verify placement of content.appendChild(description);
                    content.appendChild(description);
                    ArrayList<String> fmt = (ArrayList<String>) mline.get("fmt");

                    for (String s : fmt) {
                        String rtpMapInput = findLine(
                                media.get(i),
                                STR."a=rtpmap:\{s}",
                                null
                        );
                        org.w3c.dom.Element payloadType = doc.createElement("payload-type");
                        if (rtpMapInput != null) {
                            // add payload-type
                            var rtpMap = parseRTPMap(rtpMapInput);
                            payloadType.setAttribute("id", rtpMap.get("id").toString());
                            payloadType.setAttribute("name", rtpMap.get("name").toString());
                            payloadType.setAttribute("clockrate", rtpMap.get("clockrate").toString());
                            if (rtpMap.get("channels") != null)
                                payloadType.setAttribute("channels", rtpMap.get("channels").toString());
                            description.appendChild(payloadType);
                        }

                        String afmtpLine = findLine(media.get(i), STR."a=fmtp:\{s}", null);
                        if (afmtpLine != null) {
                            ArrayList<ParameterDataClass> ftmpParameters = parseFMTP(afmtpLine);
                            for (var val : ftmpParameters) {
                                org.w3c.dom.Element parameter = doc.createElement("parameter");
                                parameter.setAttribute("name", val.getName());
                                parameter.setAttribute("value", val.getValue());
                                payloadType.appendChild(parameter);
                            }
                        }
                        List<org.w3c.dom.Element> rtcpFbToJingleElement = rtcpFbToJingle(i, doc, (String) s);
                        if (rtcpFbToJingleElement != null) {
                            rtcpFbToJingleElement.forEach(a -> payloadType.appendChild(a));
                        }
                    }

                    List<org.w3c.dom.Element> rtcpFbToJingleElementDesc = rtcpFbToJingle(i, doc, "*");
                    if (rtcpFbToJingleElementDesc != null) {
                        rtcpFbToJingleElementDesc.forEach(a -> description.appendChild(a));
                    }

                    if (ssrc != null) {
                        HashMap<String,List<SourceParameterDataClass>> ssrcMap = parseSSRC(media.get(i));

                        for (var entry : ssrcMap.entrySet()){
                            var ssrcAttr = entry.getKey();
                            var parameters = entry.getValue();
                            org.w3c.dom.Element source = doc.createElement("source");
                            source.setAttribute("ssrc", ssrcAttr);
                            source.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:ssma:0");

                            for (var parameter : parameters){
                                org.w3c.dom.Element parameterTag = doc.createElement("parameter");
                                parameterTag.setAttribute("name", parameter.getName());
                                parameterTag.setAttribute("value", parameter.getValue());
                                source.appendChild(parameterTag);
                            }
                            description.appendChild(source);
                        }


                        //ssrc-group working fine
                        ArrayList<String> ssrcGroupLines = findLines(media.get(i), "a=ssrc-group:", null);
                        ssrcGroupLines.forEach(
                                line -> {
                                    int idx = line.indexOf(" ");
                                    String lineSemantics = line.substring(0, idx).substring(13);
                                    ArrayList<String> ssrcs = new ArrayList<String>(Arrays.asList(line.substring(14 + lineSemantics.length()).split(" ")));
                                    if (!ssrcs.isEmpty()) {
                                        org.w3c.dom.Element ssrcGroup = doc.createElement("ssrc-group");
                                        ssrcGroup.setAttribute("semantics", lineSemantics);
                                        ssrcGroup.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:ssma:0");
                                        ssrcs.forEach(
                                                ssrcData -> {
                                                    org.w3c.dom.Element ssrcElement = doc.createElement("source");
                                                    ssrcElement.setAttribute("ssrc", ssrcData);
                                                    ssrcGroup.appendChild(ssrcElement);
                                                });
                                        description.appendChild(ssrcGroup);
                                    }
                                }
                        );
                    }

                    // process a=rid:
                    boolean usesRidsForSimulcast = true;
                    ArrayList<String> ridLines = findLines(media.get(i), "a=rid:", null);
                    if (ridLines != null && !ridLines.isEmpty() && usesRidsForSimulcast) {
                        var rids = ridLines.stream()
                                .map(ridLine -> ridLine.split(":")[1])
                                .map(ridInfo -> ridInfo.split(" ")[0]);
                        org.w3c.dom.Element source = doc.createElement("source");
                        rids.forEach(rid -> {

                            source.setAttribute("rid", rid);
                            source.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:ssma:0");
                        });
                        // process a=simulcast:
                        ArrayList<String> unifiedSimulcast = findLines(media.get(i), "a=simulcast:", null);
                        if (unifiedSimulcast != null && !unifiedSimulcast.isEmpty()) {
                            org.w3c.dom.Element ridGroup = doc.createElement("rid-group");
                            ridGroup.setAttribute("semantics", "SIM");
                            ridGroup.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:ssma:0");
                            description.appendChild(ridGroup);
                            rids.forEach(rid -> {
                                org.w3c.dom.Element ridElement = doc.createElement("source");
                                ridElement.setAttribute("rid", rid);
                                ridElement.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:ssma:0");
                                description.appendChild(source);
                            });
                        }
                    }


                    String rtcpMuxElement = findLine(media.get(i), "a=rtcp-mux", null);
                    if (rtcpMuxElement != null) {
                        description.appendChild(doc.createElement("rtcp-mux"));
                    }

                    // XEP-0294 working fine
                    var extmapLines = findLines(media.get(i), "a=extmap:", session);
                    for (var line : extmapLines) {
                        var extmap = parseExtmap(line);
                        org.w3c.dom.Element rtphdrextElement = doc.createElement("rtp-hdrext");
                        rtphdrextElement.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:rtp-hdrext:0");
                        rtphdrextElement.setAttribute("uri", (String) extmap.get("uri"));
                        rtphdrextElement.setAttribute("id", (String) extmap.get("value"));

                        if (rtphdrextElement.hasAttribute("direction")) {
                            switch ((String) extmap.get("direction")) {
                                case "sendonly":
                                    rtphdrextElement.setAttribute("senders", "responder");
                                case "recvonly":
                                    rtphdrextElement.setAttribute("senders", "initiator");
                                case "sendrecv":
                                    rtphdrextElement.setAttribute("senders", "both");
                                case "inactive":
                                    rtphdrextElement.setAttribute("senders", "none");
                            }
                        }
                        description.appendChild(rtphdrextElement);
                    }
                    String extmapAllowMixed = findLine(media.get(i), "a=extmap-allow-mixed", session);
                    if (extmapAllowMixed != null) {
                        org.w3c.dom.Element extmapAllowMixedElement = doc.createElement("extmap-allow-mixed");
                        extmapAllowMixedElement.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:rtp-hdrext:0");
                        description.appendChild(extmapAllowMixedElement);
                    }
                }
                // end description tag processing

                // start transport tag processing
                org.w3c.dom.Element transportTag = doc.createElement("transport");
                transportTag.setAttribute("xmlns","urn:xmpp:jingle:transports:ice-udp:1");
                String ufrag = findLine(media.get(i),"a=ice-ufrag:",null).substring(12);
                transportTag.setAttribute("ufrag",ufrag);

                String pwd = findLine(media.get(i),"a=ice-pwd:",null).substring(10);
                transportTag.setAttribute("pwd",pwd);

                String sctpPort = findLine(media.get(i),"a=sctp-port:",null);
                String sctpMap = findLine(media.get(i),"a=sctpmap:",null);

                if(sctpPort != null){
                    var sctpAttr = parseSCTPPort(sctpPort);
                    org.w3c.dom.Element sctpMapElement = doc.createElement("sctpmap");
                    sctpMapElement.setAttribute("xmlns","urn:xmpp:jingle:transports:dtls-sctp:1");
                    sctpMapElement.setAttribute("number",sctpAttr);
                    sctpMapElement.setAttribute("protocol","webrtc-datachannel");
                    sctpMapElement.setAttribute("streams","0");
                    transportTag.appendChild(sctpMapElement);
                } else if (sctpMap != null) {
                    var sctpAttr = parseSCTPMap(sctpMap);
                    org.w3c.dom.Element sctpMapElement = doc.createElement("sctpmap");
                    sctpMapElement.setAttribute("xmlns","urn:xmpp:jingle:transports:dtls-sctp:1");
                    sctpMapElement.setAttribute("number",sctpAttr.get(0));
                    sctpMapElement.setAttribute("protocol",sctpAttr.get(1));
                    sctpMapElement.setAttribute("streams",sctpAttr.get(2));
                    transportTag.appendChild(sctpMapElement);
                }

                var fingerprints = findLines(media.get(i),"a=fingerprint:",session);
                for(var ele : fingerprints){
                    var fingerprint = parseFingerprint(ele);
                    var fingerprintElement = doc.createElement("fingerprint");
                    var setup = findLine(media.get(i),"a=setup:",session);
                    if(setup != null){
                        fingerprint.put("setup",setup.substring(8));
                    }
                    fingerprintElement.setAttribute("xmlns","urn:xmpp:jingle:apps:dtls:0");
                    fingerprintElement.setAttribute("hash",(String)fingerprint.get("hash"));
                    fingerprintElement.setAttribute("setup",(String)fingerprint.get("setup"));
                    fingerprintElement.setAttribute("required","false");
                    fingerprintElement.setTextContent((String)fingerprint.get("fingerprint"));
                    transportTag.appendChild(fingerprintElement);
                }
                content.appendChild(transportTag);

                var iceParams = parseICEParams(media.get(i),session);

                if(iceParams != null){
                    var candidateLines = findLines(media.get(i),"a=candidate:",session);
                    candidateLines.forEach(
                            ele -> {
                                var candidate = candidateToJingle(ele);
                                if(failICE){
                                    candidate.put("ip","1.1.1.1");
                                }
                                String protocol = candidate.get("protocol").toString().toLowerCase();


                                if((removeTcpCandidates && (protocol.equals("tcp") || protocol.equals("ssltcp"))) || (removeTcpCandidates && protocol.equals("udp"))){
                                    return;
                                }

                                org.w3c.dom.Element candidateElement = doc.createElement("candidate");
                                candidateElement.setAttribute("foundation",candidate.get("foundation"));
                                candidateElement.setAttribute("component",candidate.get("component"));
                                candidateElement.setAttribute("protocol",candidate.get("protocol"));
                                candidateElement.setAttribute("priority",candidate.get("priority"));
                                candidateElement.setAttribute("ip",candidate.get("ip"));
                                candidateElement.setAttribute("port",candidate.get("port"));
                                candidateElement.setAttribute("type",candidate.get("type"));
                                candidateElement.setAttribute("generation",candidate.get("generation"));
                                if(candidate.containsKey("rel-addr")) candidateElement.setAttribute("rel-addr",candidate.get("rel-addr"));
                                if(candidate.containsKey("rel-port")) candidateElement.setAttribute("rel-port",candidate.get("rel-port"));

                                // network attribute set as 1 in lib-jitsi-meet
                                candidateElement.setAttribute("network","0");
                                candidateElement.setAttribute("id",UUID.randomUUID().toString());
                                transportTag.appendChild(candidateElement);
                            }
                    );
                }
                // end transport tag processing

                var m = media.get(i);

                if(findLine(m,"a=sendrecv",null) != null){
                    content.setAttribute("senders","both");
                } else if(findLine(m,"a=sendonly",null) != null){
                    content.setAttribute("senders","initiator");
                }else if(findLine(m,"a=recvonly",null) != null){
                    content.setAttribute("senders","responder");
                }else if(findLine(m,"a=inactive",null) != null){
                    content.setAttribute("senders","none");
                }
                if(mline.get("port").toString().equals("0") && findLine(m,"a=bundle-only",session) != null){
                    content.setAttribute("senders","rejected");
                }
                jingleElement.appendChild(content);
            }


            jingleElement.appendChild(group);
        }

        doc.appendChild(rootElement);


        // output file as xml
        try {
            FileOutputStream output = new FileOutputStream(".\\staff-dom.xml");
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (Exception e) {
            System.out.println(e);
        }

        return "";
    }

    public Map<String,String> candidateToJingle(java.lang.String line){
        if(line.indexOf("candidate:") == 0){
            line = STR."a=\{line}";
        } else if (!line.startsWith("a=candidate:")) {
            return null;
        }
        if(line.substring(line.length()-2).equals("\r\n")){
            line = line.substring(0,line.length()-2);
        }
        Map candidate = new HashMap<>();
        var elems = line.split(" ");
        if(!elems[6].equals("typ")){
            return null;
        }

        candidate.put("foundation",elems[0].substring(12));
        candidate.put("component",elems[1]);
        candidate.put("protocol",elems[2].toLowerCase());
        candidate.put("priority",elems[3]);
        candidate.put("ip",elems[4]);
        candidate.put("port",elems[5]);
        candidate.put("type",elems[7]);
        candidate.put("generation","0");


        for (int i = 0;i< elems.length;i++){
            switch (elems[i]){
                case "raddr" :
                    candidate.put("rel-addr",elems[i+1]);
                    break;
                case "rport" :
                    candidate.put("rel-port",elems[i+1]);
                    break;
                case "generation" : candidate.put("generation",elems[i+1]);
                    break;
                case "tcptype" : candidate.put("tcptype",elems[i+1]);
                    break;
                default:
                    break;
            }
        }
        candidate.put("network","1");
        candidate.put("id",GenerateRandomNumber(8));
        return candidate;
    }

    String GenerateRandomNumber(int charLength) {
        return String.valueOf(charLength < 1 ? 0 : new Random()
                .nextInt((9 * (int) Math.pow(10, charLength - 1)) - 1)
                + (int) Math.pow(10, charLength - 1));
    }

    public Map parseICEParams(String media,String session){
        Map data = new HashMap<>();
        String pwd = findLine(media,"a=ice-pwd:",session);
        String ufrag = findLine(media,"a=ice-ufrag:",session);

        if(ufrag != null && pwd != null){
            data.put("ufrag" , ufrag.substring(12));
            data.put("pwd",pwd.substring(10));
        }
        return data;
    }
    public Map parseFingerprint(String line){
        Map data = new HashMap<>();
        var parts = new ArrayList<>(Arrays.asList(line.substring(14).split(" ")));
        data.put("hash" , parts.getFirst());
        parts.removeFirst();
        data.put("fingerprint" , parts.getFirst());
        parts.removeFirst();

        return data;
    }
    public ArrayList<String> parseSCTPMap(String line){
        ArrayList<String> data = new ArrayList<>();
        var parts = new ArrayList<>(Arrays.asList(line.substring(10).split(" ")));
        String sctpPort = parts.get(0);
        data.add(sctpPort);
        String protocol = parts.get(1);
        data.add(protocol);
        String streamCount = parts.size() > 2 ? parts.get(2) : "0";
        return data;
    }
    public String parseSCTPPort(String line){
        return line.substring(12);
    }
    public Map parseExtmap(String line) {
        var parts = new ArrayList<>(Arrays.asList(line.substring(9).split(" ")));
        Map data = new HashMap<>();
        data.put("value", parts.getFirst());
        parts.removeFirst();

        if (((String) data.get("value")).indexOf("/") == -1) {
            data.put("direction", "both");
        } else {
            String value = (String) data.get("value");
            data.put("direction", value.substring(value.indexOf("/") + 1));
            data.put("value", value.substring(0, value.indexOf("/")));
        }
        data.put("uri", parts.getFirst());
        parts.removeFirst();

        data.put("params", parts);
        return data;
    }
    public String parseMSIDAttributes(ArrayList<String> ssrcLines) {
        String msidLine = "";
        for (var ssrcSdpLine : ssrcLines) {
            if (ssrcSdpLine.indexOf(" msid:") > 0) {
                msidLine = ssrcSdpLine;
                break;
            }
        }
        return msidLine.substring(msidLine.indexOf(" msid") + 6);

    }
    public String parseSourceNameLine(ArrayList<String> ssrcLines) {
        for(var line : ssrcLines){
            if(line.indexOf(" name:") > 0){
                return line.substring(line.indexOf(" name:") + 6);
            }
        }
        return null;
    }
    public String parseVideoType(ArrayList<String> ssrcLines) {
        String s = " videoType:";
        String videoTypeLine = "";
        for (var ssrcSdpLine : ssrcLines) {
            if (ssrcSdpLine.indexOf("s") > 0) {
                videoTypeLine = ssrcSdpLine;
                break;
            }
        }
        return videoTypeLine.substring(videoTypeLine.indexOf(s) + s.length());
    }
    public HashMap<String,List<SourceParameterDataClass>> parseSSRC(String desc) {
        var lines = new ArrayList<>(Arrays.asList(desc.split("\n")));
        HashMap<String,List<SourceParameterDataClass>> result = new HashMap<>();

        for (var line : lines){
            if(line.startsWith("a=ssrc:")){
                var parts = new ArrayList<String>(Arrays.asList(line.split(" ")));
                var ssrcValue = parts.get(0).substring(7);
                parts.removeFirst();
                var parameterParts = String.join(" ",parts).split(":");
                var parameterTag = new SourceParameterDataClass(parameterParts[0], parameterParts[1]);
                if(result.containsKey(ssrcValue)){
                    var dataValue = result.get(ssrcValue);
                    dataValue.add(parameterTag);
                    result.put(ssrcValue,dataValue);
                }else{
                    List<SourceParameterDataClass> initialise = new ArrayList<>(Collections.emptyList());
                    initialise.add(parameterTag);
                    result.put(ssrcValue, initialise);
                }

            }
        }

        return result;
    }
    public List<org.w3c.dom.Element> rtcpFbToJingle(int i, org.w3c.dom.Document doc, String payload) {
        ArrayList<String> lines = findLines(media.get(i), STR."a=rtcp-fb:\{payload}", null);
        List<org.w3c.dom.Element> result = new ArrayList<>();
        for (var line : lines) {
            RTCPFBDataClass feedback = parseRTCPFB(line);
            if (feedback.getType().equals("trr-int")) {
                org.w3c.dom.Element rtcpFB = doc.createElement("rtcp-fb-trr-int");
                rtcpFB.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:rtcp-fb:0");
                rtcpFB.setAttribute("value", feedback.getParams().getFirst());
                result.add(rtcpFB);
            } else {
                org.w3c.dom.Element rtcpFB = doc.createElement("rtcp-fb");
                rtcpFB.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:rtcp-fb:0");
                rtcpFB.setAttribute("type", feedback.getType());
                if (!feedback.getParams().isEmpty()) {
                    rtcpFB.setAttribute("subtype", feedback.getParams().getFirst());
                }
                result.add(rtcpFB);
            }
        }
        return result;
    }
    public RTCPFBDataClass parseRTCPFB(String line) {
        var parts = new ArrayList<>(Arrays.asList(line.substring(9).split(" ")));
        var pt = parts.getFirst();
        parts.removeFirst();
        var type = parts.getFirst();
        parts.removeFirst();
        var params = parts;
        return new RTCPFBDataClass(pt, type, params);
    }
    public ArrayList<ParameterDataClass> parseFMTP(String data) {
        ArrayList<ParameterDataClass> result = new ArrayList<>();
        ArrayList<String> parts = new ArrayList<>(Arrays.asList(data.split(" ")));
        parts.removeFirst();

        ArrayList<String> tempList;
        tempList = parts;
        StringBuilder tempString = new StringBuilder();

        for (var part : tempList) {
            tempString.append(STR."\{part} ");
        }
        parts = new ArrayList<>(Arrays.asList(tempString.toString().split(";")));

        for (int i = 0; i < parts.size(); i++) {
            if (!parts.get(i).equals("")) {
                String key = "";
                String value = "";
                if (parts.get(i).contains("=")){
                    key = parts.get(i).split("=")[0];
                    value = parts.get(i).split("=")[1];
                }else{
                    value = parts.get(i);
                }

                result.add(new ParameterDataClass(key.trim(), value.trim()));
            }
        }
        return result;
    }
    public Map<String, Object> parseRTPMap(String data) {
        Map<String, Object> result = new HashMap<>();
        ArrayList<String> parts = new ArrayList<>(Arrays.asList(data.substring(9).split(" ")));
        result.put("id", parts.getFirst());
        parts.removeFirst();
        parts = new ArrayList<>(Arrays.asList(parts.getFirst().split("/")));
        result.put("name", parts.getFirst());
        parts.removeFirst();
        result.put("clockrate", parts.getFirst());
        parts.removeFirst();

        if (!parts.isEmpty()) {
            result.put("channels", parts.getFirst());
            parts.removeFirst();
        }
        return result;
    }
    public Map<String, Object> parseMLine(String data) {
        Map<String, Object> result = new HashMap<>();
        ArrayList<String> parts = new ArrayList<>(Arrays.asList(data.substring(2).split(" ")));
        result.put("media", parts.getFirst());
        parts.removeFirst();
        result.put("port", parts.getFirst());
        parts.removeFirst();
        result.put("proto", parts.getFirst());
        parts.removeFirst();

        if (parts.getLast().equals("")) {
            parts.removeLast();
        }
        ArrayList<Object> fmt = new ArrayList<>();
        parts.forEach(part -> fmt.add(part.trim()));
        result.put("fmt", fmt);
        parts.removeFirst();
        return result;
    }
    public String findLine(String haystack, String needle, String sessionPart) {
        String[] lines = haystack.split("\n");
        ArrayList<String> needles = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(needle)) {
                return line;
            }
        }

        if (sessionPart == null) {
            return null;
        }

        lines = sessionPart.split("\n");
        for (String line : lines) {
            if (line.startsWith(needle)) {
                return line;
            }
        }
        return null;
    }
    public ArrayList<String> findLines(String haystack, String needle, String sessionPart) {
        String[] lines = haystack.split("\n");
        ArrayList<String> needles = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(needle)) {
                needles.add(line);
            }
        }

        if (sessionPart != null) {
            lines = sessionPart.split("\n");
            for (String line : lines) {
                if (line.startsWith(needle)) {
                    needles.add(line);
                }
            }
        }
        return needles;
    }
}

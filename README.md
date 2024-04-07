# Jicofo-KMS-Bridge (JKB)
Project Name: Sangoshthee  
Duration: 1st January 2024 - 1st April 2024 (3 months)  
Location: Hybrid Mode: Online / C-DAC,Pune

## Introduction
This repository is a result of the internship that I had done at Centre for Development of Advanced Computing (CDAC) from 01 January 2024 to April 2024 under the mentorship of Mr. Milind Jagtap and Mr. Deepanshu Rautela.

This repository is a helper block of a project that attempts to dynamically change the architecture of how the video streams are handled by the Jitsi stack.
Following are some of the functions of this block:

1. Listens for different jingle actions (JingleIQ) on an XMPP connection.
2. Convert JingleIQ to SDP
3. Convert SDP to JingleIQ

## Dataflow
[![dataflow](https://github.com/vedantbarve/Jicofo-KMS-Bridge/blob/final/assets/dataflow.png?raw=true)

1. We invite Jicofo to an MUC by sending it a ConferenceIQ.
2. If Jicofo is not a of that MUC, then it joins the MUC, else it continues
3. JKB joins the MUC if and only if Jicofo is a part of that MUC.
4. Jicofo initiates jingle session with JKB by sending session-initiate JingleIQ to JKB.
5. JKB first converts this session-initiate to SDP and then it to RTP-SSRC-DEMUX via websocket (sending the SDP via websockets has not been accounted yet).
6. RTP-SSRC-DEMUX processes this data and reproduces an SDP which is received by JKB via websocket (receiving the SDP via websockets has not been accounted yet).
7. This received SDP consists data for session-accept which is converted to session-accept JingleIQ at JKB.
8. This session-accept JingleIQ is sent to Jicofo.
9. If the response is successful, a jingle session between JKB and Jicofo is established.

## Contact
For any further details, feel free to contact me via email : barvevedant@gmail.com

## Last Modified
07-04-2024

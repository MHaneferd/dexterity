//package com.ht1.dexterity.app;

// This is a struct that is supsoed to tell the protocol version and is the first that the client is sending
// The complete protocol is:
// 	1) the client connects
//  2) send this message.
//  3) the server will send numberOfRecords of type TransmitterRawData that it has.
public class ComunicationHeader  {
    int version;
    int numberOfRecords;
    String message;
    byte reserved[];
}

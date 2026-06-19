package com.polishbank.bank_a.integration.swift;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class SwiftPacs008Builder {

    private static final String NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public BuiltMessage build(MessageInput input) {
        String uetr = UUID.randomUUID().toString();
        String suffix = uetr.substring(0, 8).toUpperCase();
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DATETIME);
        String today = LocalDate.now(ZoneOffset.UTC).format(ISO_DATE);
        String messageId = "MSG-" + suffix;
        String instructionId = "INST-" + suffix;
        String amount = input.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();

        StringBuilder xml = new StringBuilder(1024);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Document xmlns=\"").append(NAMESPACE).append("\">");
        xml.append("<FIToFICstmrCdtTrf>");
        xml.append("<GrpHdr>");
        xml.append("<MsgId>").append(escape(messageId)).append("</MsgId>");
        xml.append("<CreDtTm>").append(now).append("</CreDtTm>");
        xml.append("<NbOfTxs>1</NbOfTxs>");
        xml.append("<SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>");
        xml.append("</GrpHdr>");
        xml.append("<CdtTrfTxInf>");
        xml.append("<PmtId>");
        xml.append("<InstrId>").append(escape(instructionId)).append("</InstrId>");
        xml.append("<EndToEndId>NOTPROVIDED</EndToEndId>");
        xml.append("<UETR>").append(uetr).append("</UETR>");
        xml.append("</PmtId>");
        xml.append("<IntrBkSttlmAmt Ccy=\"").append(escape(input.currency())).append("\">")
                .append(amount).append("</IntrBkSttlmAmt>");
        xml.append("<IntrBkSttlmDt>").append(today).append("</IntrBkSttlmDt>");
        xml.append("<InstdAmt Ccy=\"").append(escape(input.currency())).append("\">")
                .append(amount).append("</InstdAmt>");
        xml.append("<ChrgBr>").append(escape(input.chargeBearer())).append("</ChrgBr>");
        xml.append("<InstgAgt><FinInstnId><BICFI>").append(escape(input.senderBic()))
                .append("</BICFI></FinInstnId></InstgAgt>");
        xml.append("<InstdAgt><FinInstnId><BICFI>").append(escape(input.receiverBic()))
                .append("</BICFI></FinInstnId></InstdAgt>");
        xml.append("<Dbtr><Nm>").append(escape(input.senderName())).append("</Nm></Dbtr>");
        xml.append("<DbtrAcct><Id><IBAN>").append(escape(input.senderIban())).append("</IBAN></Id></DbtrAcct>");
        xml.append("<DbtrAgt><FinInstnId><BICFI>").append(escape(input.senderBic()))
                .append("</BICFI></FinInstnId></DbtrAgt>");
        xml.append("<CdtrAgt><FinInstnId><BICFI>").append(escape(input.receiverBic()))
                .append("</BICFI></FinInstnId></CdtrAgt>");
        xml.append("<Cdtr><Nm>").append(escape(input.receiverName())).append("</Nm>");
        if (input.receiverCountry() != null && !input.receiverCountry().isBlank()) {
            xml.append("<PstlAdr><Ctry>").append(escape(input.receiverCountry())).append("</Ctry></PstlAdr>");
        }
        xml.append("</Cdtr>");
        xml.append("<CdtrAcct><Id>");
        if (looksLikeIban(input.receiverIban())) {
            xml.append("<IBAN>").append(escape(input.receiverIban())).append("</IBAN>");
        } else {
            xml.append("<Othr><Id>").append(escape(input.receiverIban())).append("</Id></Othr>");
        }
        xml.append("</Id></CdtrAcct>");
        if (input.title() != null && !input.title().isBlank()) {
            xml.append("<RmtInf><Ustrd>").append(escape(input.title())).append("</Ustrd></RmtInf>");
        }
        xml.append("</CdtTrfTxInf>");
        xml.append("</FIToFICstmrCdtTrf>");
        xml.append("</Document>");

        return new BuiltMessage(xml.toString(), uetr, messageId, instructionId);
    }

    private boolean looksLikeIban(String value) {
        if (value == null || value.length() < 15) {
            return false;
        }
        return Character.isLetter(value.charAt(0)) && Character.isLetter(value.charAt(1));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public record MessageInput(
            String senderBic,
            String senderName,
            String senderIban,
            String receiverBic,
            String receiverName,
            String receiverIban,
            String receiverCountry,
            BigDecimal amount,
            String currency,
            String chargeBearer,
            String title
    ) {}

    public record BuiltMessage(String xml, String uetr, String messageId, String instructionId) {}
}
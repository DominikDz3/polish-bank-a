package com.polishbank.bank_a.integration.payments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IsoXml {

    private IsoXml() {}

    public static String buildPacs008(
            String paymentId,
            String senderBicfi,
            String receiverBicfi,
            String senderAccountIban,
            String receiverAccountIban,
            String senderName,
            String receiverName,
            BigDecimal amount,
            String currency,
            String title,
            String serviceCode
    ) {
        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amt = amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();

        return """
                <Document>
                    <FIToFICstmrCdtTrf>
                        <GrpHdr>
                            <MsgId>%s</MsgId>
                            <CreDtTm>%s</CreDtTm>
                            <NbOfTxs>1</NbOfTxs>
                            <TtlIntrBkSttlmAmt Ccy="%s">%s</TtlIntrBkSttlmAmt>
                            <SttlmInf>
                                <SttlmMtd>CLRG</SttlmMtd>
                                <ClrSys><Cd>%s</Cd></ClrSys>
                            </SttlmInf>
                        </GrpHdr>
                        <CdtTrfTxInf>
                            <PmtId>
                                <InstrId>%s</InstrId>
                                <EndToEndId>%s</EndToEndId>
                                <TxId>%s</TxId>
                            </PmtId>
                            <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>
                            <Dbtr><Nm>%s</Nm></Dbtr>
                            <DbtrAcct><Id><IBAN>%s</IBAN></Id></DbtrAcct>
                            <DbtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></DbtrAgt>
                            <Cdtr><Nm>%s</Nm></Cdtr>
                            <CdtrAcct><Id><IBAN>%s</IBAN></Id></CdtrAcct>
                            <CdtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></CdtrAgt>
                            <RmtInf><Ustrd>%s</Ustrd></RmtInf>
                            <SplmtryData>
                                <Envlp>
                                    <ServiceCode>%s</ServiceCode>
                                    <SenderBankId>%s</SenderBankId>
                                    <ReceiverBankId>%s</ReceiverBankId>
                                </Envlp>
                            </SplmtryData>
                        </CdtTrfTxInf>
                    </FIToFICstmrCdtTrf>
                </Document>
                """.formatted(
                paymentId, createdAt, currency, amt, serviceCode,
                paymentId, paymentId, paymentId,
                currency, amt,
                escape(senderName), senderAccountIban, senderBicfi,
                escape(receiverName), receiverAccountIban, receiverBicfi,
                escape(title),
                serviceCode, senderBicfi, receiverBicfi
        );
    }

    private static final Pattern STATUS_PATTERN = Pattern.compile("<TxSts>([^<]+)</TxSts>");
    private static final Pattern REASON_PATTERN = Pattern.compile("<AddtlInf>([^<]+)</AddtlInf>");

    public static ParsedResponse parsePain002(String xml) {
        Matcher s = STATUS_PATTERN.matcher(xml);
        Matcher r = REASON_PATTERN.matcher(xml);
        String status = s.find() ? s.group(1) : "UNKNOWN";
        String reason = r.find() ? r.group(1) : null;
        return new ParsedResponse(status, reason);
    }

    public record ParsedResponse(String status, String reason) {}

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
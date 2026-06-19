package com.polishbank.bank_a.integration.swift;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;

@Component
public class SwiftInboundParser {

    public ParsedMessage parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();

            String uetr = textOf(root, "UETR");
            String messageId = textOf(root, "MsgId");
            String chargeBearer = textOf(root, "ChrgBr");
            String receiverIban = ibanFromCdtrAcct(root);
            String senderBic = bicFromAgent(root, "DbtrAgt");
            String receiverBic = bicFromAgent(root, "CdtrAgt");
            String returnReason = textOf(root, "Rsn");
            String remittance = textOf(root, "Ustrd");

            BigDecimal amount = decimalOf(root, "IntrBkSttlmAmt");
            if (amount == null) {
                amount = decimalOf(root, "InstdAmt");
            }
            String currency = currencyOf(root);

            return new ParsedMessage(
                    uetr, messageId, senderBic, receiverBic, receiverIban,
                    amount, currency, chargeBearer, returnReason, remittance);
        } catch (Exception e) {
            throw new IllegalArgumentException("Niepoprawny komunikat SWIFT: " + e.getMessage(), e);
        }
    }

    private String textOf(Element root, String tagName) {
        NodeList list = root.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return null;
        }
        String text = list.item(0).getTextContent();
        return text == null ? null : text.trim();
    }

    private String bicFromAgent(Element root, String agentTag) {
        NodeList agents = root.getElementsByTagName(agentTag);
        for (int i = 0; i < agents.getLength(); i++) {
            Element agent = (Element) agents.item(i);
            NodeList bics = agent.getElementsByTagName("BICFI");
            if (bics.getLength() > 0) {
                String text = bics.item(0).getTextContent();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String ibanFromCdtrAcct(Element root) {
        NodeList accts = root.getElementsByTagName("CdtrAcct");
        if (accts.getLength() == 0) {
            return null;
        }
        Element acct = (Element) accts.item(0);
        NodeList ibans = acct.getElementsByTagName("IBAN");
        if (ibans.getLength() > 0) {
            String text = ibans.item(0).getTextContent();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        NodeList others = acct.getElementsByTagName("Id");
        for (int i = others.getLength() - 1; i >= 0; i--) {
            Element id = (Element) others.item(i);
            String parentName = id.getParentNode() == null ? "" : id.getParentNode().getNodeName();
            if ("Othr".equals(parentName)) {
                String text = id.getTextContent();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private BigDecimal decimalOf(Element root, String tag) {
        NodeList list = root.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return null;
        }
        String text = list.item(0).getTextContent();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String currencyOf(Element root) {
        NodeList list = root.getElementsByTagName("IntrBkSttlmAmt");
        if (list.getLength() == 0) {
            list = root.getElementsByTagName("InstdAmt");
        }
        if (list.getLength() == 0) {
            return null;
        }
        Element el = (Element) list.item(0);
        String ccy = el.getAttribute("Ccy");
        return ccy == null || ccy.isBlank() ? null : ccy;
    }

    public record ParsedMessage(
            String uetr,
            String messageId,
            String senderBic,
            String receiverBic,
            String receiverIban,
            BigDecimal amount,
            String currency,
            String chargeBearer,
            String returnReason,
            String remittanceInfo
    ) {}
}
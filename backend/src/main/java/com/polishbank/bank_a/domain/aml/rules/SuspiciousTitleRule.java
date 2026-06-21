package com.polishbank.bank_a.domain.aml.rules;

import com.polishbank.bank_a.domain.aml.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SuspiciousTitleRule implements AmlRule {
    private static final List<String> KEYWORDS = List.of(
        "crypto", "bitcoin", "btc", "darknet", "cash only", "kasyno", "casino",
        "donation", "donacja", "loan refund", "spłata pożyczki", "investment scheme"
    );

    @Override public String code() { return "SUSPICIOUS_TITLE"; }

    @Override
    public AmlResult check(AmlContext ctx, AmlSupportData support) {
        if (ctx.title() == null) return AmlResult.ok();
        String lower = ctx.title().toLowerCase();
        for (String kw : KEYWORDS) {
            if (lower.contains(kw)) {
                return AmlResult.hold(code(),
                    "Tytuł przelewu zawiera frazę oznaczoną jako podejrzana: \"" + kw + "\".");
            }
        }
        return AmlResult.ok();
    }
}
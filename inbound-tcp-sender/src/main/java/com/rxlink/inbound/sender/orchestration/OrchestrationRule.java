package com.rxlink.inbound.sender.orchestration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrchestrationRule {

    private String prefixBase64 = "";
    private String suffixBase64 = "";

    public String getPrefixBase64() {
        return prefixBase64;
    }

    public void setPrefixBase64(String prefixBase64) {
        this.prefixBase64 = prefixBase64;
    }

    public String getSuffixBase64() {
        return suffixBase64;
    }

    public void setSuffixBase64(String suffixBase64) {
        this.suffixBase64 = suffixBase64;
    }
}

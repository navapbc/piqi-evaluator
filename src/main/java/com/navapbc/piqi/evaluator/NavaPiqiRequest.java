package com.navapbc.piqi.evaluator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NavaPiqiRequest {
    @JsonProperty("dataProviderID")
    private String dataProviderId;
    @JsonProperty("dataSourceID")
    private String dataSourceId;
    @JsonProperty("evaluationRubricMnemonic")
    private String evaluationRubricMnemonic;
    @JsonProperty("messageData")
    private String messageData;
    @JsonProperty("MessageId")
    private String messageId;
    @JsonProperty("piqiModelMnemonic")
    private String piqiModelMnemonic;
}
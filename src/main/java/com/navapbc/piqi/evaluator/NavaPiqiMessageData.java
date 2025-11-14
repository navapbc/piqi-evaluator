package com.navapbc.piqi.evaluator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navapbc.piqi.model.PiqiPatient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NavaPiqiMessageData {
    @JsonProperty("FormatID")
    private String formatId;
    @JsonProperty("MessageId")
    private String messageId;
    @JsonProperty("Patient")
    private PiqiPatient patient;
}
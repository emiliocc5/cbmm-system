package com.processor.infrastructure.adapters.in.http.dto;


import lombok.Value;

@Value
public class EventDTO {
    String event_id;
    String event_type;
    String operation_date;
    AccountDTO origin;
    AccountDTO destination;
}

package com.pimak.hanab.client.protocol;

import java.util.Map;

/** A command sent to hanab.live. Payload keys are wire-format camelCase names. */
public interface OutboundCommand {
    String commandName();
    Map<String, Object> payload();
}

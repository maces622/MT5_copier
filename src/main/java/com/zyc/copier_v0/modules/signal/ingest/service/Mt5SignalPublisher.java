package com.zyc.copier_v0.modules.signal.ingest.service;

import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;

public interface Mt5SignalPublisher {

    void publish(NormalizedMt5Signal signal);
}

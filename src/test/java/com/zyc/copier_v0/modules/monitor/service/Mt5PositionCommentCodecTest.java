package com.zyc.copier_v0.modules.monitor.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class Mt5PositionCommentCodecTest {

    @Test
    void shouldParseTrackingFieldsFromTaggedComment() {
        Map<String, Long> fields = Mt5PositionCommentCodec.parseTrackingFields("cp1|mp=35954038|mo=123456");

        assertThat(fields)
                .containsEntry("mp", 35954038L)
                .containsEntry("mo", 123456L);
    }

    @Test
    void shouldIgnoreMalformedOrNonTrackingComments() {
        assertThat(Mt5PositionCommentCodec.parseTrackingFields("manual-order")).isEmpty();
        assertThat(Mt5PositionCommentCodec.parseTrackingFields("cp|mp=1")).isEmpty();
        assertThat(Mt5PositionCommentCodec.parseTrackingFields("cp1|mp=abc|mo=2"))
                .containsEntry("mo", 2L)
                .doesNotContainKey("mp");
    }
}

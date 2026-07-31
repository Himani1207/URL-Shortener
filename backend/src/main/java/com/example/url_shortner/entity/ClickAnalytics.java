package com.example.url_shortner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One recorded click on a short link.
 *
 * <p><b>Relationships and columns are unchanged.</b> The only addition is a composite
 * index on {@code (url_id, clicked_at)}: every analytics query filters by link and
 * orders or ranges on time, and this is the table that grows without bound. Adding
 * it later, once there are millions of rows, means a long lock on a live table.
 *
 * <p>Values are written by {@code UrlServiceImpl} from
 * {@link com.example.url_shortner.util.UserAgentParser} and
 * {@link com.example.url_shortner.util.ClientIpResolver}. Both cap their output at
 * the column width, so no value can overflow — worth noting because the old code
 * stored the entire raw {@code User-Agent} header in {@code browser}, which routinely
 * exceeds 255 characters.
 */
@Entity
@Table(
        name = "click_analytics",
        indexes = {
                @Index(name = "idx_click_url_clicked_at", columnList = "url_id, clicked_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Best-effort origin address; see {@code ClientIpResolver} on trust. */
    private String ipAddress;

    private String browser;

    private String operatingSystem;

    /** One of Desktop / Mobile / Tablet / Bot / Unknown. */
    private String device;

    private LocalDateTime clickedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id")
    private Url url;

    @PrePersist
    public void prePersist() {
        clickedAt = LocalDateTime.now();
    }
}

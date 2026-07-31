package com.example.url_shortner.service;

/**
 * Renders QR codes for short links.
 *
 * <p><b>Where it belongs:</b> the service layer, alongside {@code UrlService}.
 * It is a separate bean rather than another method on {@code UrlService} because
 * image rendering is an unrelated responsibility with its own failure modes and
 * its own cache region — {@code UrlService} should not grow a ZXing dependency.
 *
 * <p><b>On storage:</b> the brief allowed for persisting generated images. They are
 * cached in Redis instead of being written to the database or a blob store, because
 * a QR code is a pure function of the short link: it is fully regenerable in about
 * a millisecond, needs no backup, and storing binary blobs in Postgres would bloat
 * the row store and every backup for no benefit. The Redis entry is evicted
 * together with the URL entry whenever the link changes.
 */
public interface QrCodeService {

    /**
     * Returns the PNG for a short code at the default size, served from cache when
     * available.
     *
     * @param shortCode the code to encode; the full short URL is built from the
     *                  configured public base URL
     * @return PNG bytes, never {@code null}
     */
    byte[] getQrCodeForShortCode(String shortCode);

    /**
     * Renders a PNG at an explicit size, bypassing the cache.
     *
     * <p>Only the default size is cached — caching every requested dimension would
     * let a caller fill Redis with arbitrarily many variants of the same code, and
     * would break eviction, which keys strictly on the short code.
     *
     * @param shortCode the code to encode
     * @param size      width and height in pixels
     */
    byte[] renderQrCode(String shortCode, int size);

    /** @return the default edge length in pixels used by {@link #getQrCodeForShortCode}. */
    int defaultSize();
}

package com.example.url_shortner.service.impl;

import com.example.url_shortner.cache.CacheNames;
import com.example.url_shortner.exception.QrCodeGenerationException;
import com.example.url_shortner.service.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * ZXing-backed implementation of {@link QrCodeService}.
 *
 * <p>The encoded payload is the public short link ({@code {base-url}/{shortCode}}),
 * not the original destination — that is the whole point of a shortener QR code:
 * scans have to route through the redirect so they are counted in analytics.
 */
@Slf4j
@Service
public class QrCodeServiceImpl implements QrCodeService {

    /** Bounds on the {@code size} query parameter, to stop absurd allocations. */
    private static final int MIN_SIZE = 64;
    private static final int MAX_SIZE = 1000;

    /** Quiet-zone width in modules. */
    private static final int MARGIN = 1;

    private final String baseUrl;
    private final int defaultSize;

    public QrCodeServiceImpl(
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.qr.default-size:300}") int defaultSize) {

        // Trailing slashes would produce "https://host//code" in the encoded payload.
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultSize = clampSize(defaultSize);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Keyed on the short code alone so that
     * {@code UrlCacheService.evict(shortCode)} can invalidate it with the same key.
     */
    @Override
    @Cacheable(cacheNames = CacheNames.QR_CODE, key = "#shortCode")
    public byte[] getQrCodeForShortCode(String shortCode) {
        log.debug("Cache MISS [cache={}, shortCode={}] - rendering QR code",
                CacheNames.QR_CODE, shortCode);
        return renderQrCode(shortCode, defaultSize);
    }

    @Override
    public byte[] renderQrCode(String shortCode, int size) {
        int edge = clampSize(size);
        String target = baseUrl + "/" + shortCode;

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        // Level M recovers ~15% damage: the usual balance between resilience and
        // module density for a short URL payload.
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, MARGIN);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            BitMatrix matrix = new QRCodeWriter()
                    .encode(target, BarcodeFormat.QR_CODE, edge, edge, hints);

            MatrixToImageWriter.writeToStream(matrix, "PNG", output);

            byte[] png = output.toByteArray();
            log.debug("Rendered QR code [shortCode={}, size={}px, bytes={}]",
                    shortCode, edge, png.length);
            return png;

        } catch (WriterException | IOException ex) {
            log.error("Failed to render QR code [shortCode={}, size={}]", shortCode, edge, ex);
            throw new QrCodeGenerationException(
                    "Unable to generate QR code for short code: " + shortCode, ex);
        }
    }

    @Override
    public int defaultSize() {
        return defaultSize;
    }

    /** Keeps requested dimensions inside a sane range instead of rejecting them. */
    private int clampSize(int requested) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, requested));
    }
}

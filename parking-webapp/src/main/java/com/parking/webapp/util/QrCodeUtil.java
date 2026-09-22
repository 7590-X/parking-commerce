package com.parking.webapp.util;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * Utilidad para la generación de códigos QR en formato Base64 para tickets físicos y visualización.
 */
@Slf4j
public final class QrCodeUtil {

    private QrCodeUtil() {
    }

    /**
     * Genera una imagen PNG en Base64 con el contenido especificado.
     *
     * @param text   Texto o UUID a codificar en el QR.
     * @param width  Ancho en píxeles.
     * @param height Alto en píxeles.
     * @return Cadena URI en formato "data:image/png;base64,...", o cadena vacía en caso de error.
     */
    public static String generateQrBase64(String text, int width, int height) {
        if (text == null || text.isBlank()) {
            return "";
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            log.error("Error al generar código QR para el texto [{}]: {}", text, e.getMessage());
            return "";
        }
    }
}

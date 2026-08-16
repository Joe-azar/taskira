package com.joe.taskira.exports.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Encodes a URL into a QR code PNG - used to embed a scannable link back to a ticket's
 * web page in the single-ticket PDF report (TicketPdfExportService). Decoding the same
 * image back to text (proving the embedded QR really is scannable, not just present) is
 * exercised by tests directly against ZXing's MultiFormatReader, not duplicated here.
 */
@Component
public class QrCodeGenerator {

    private static final int SIZE_PX = 200;

    public byte[] generatePng(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE_PX, SIZE_PX);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }

    public String generateDataUri(String content) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(generatePng(content));
    }
}

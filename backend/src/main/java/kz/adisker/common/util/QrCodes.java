package kz.adisker.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/** Генерация QR-кодов в виде data-URL (PNG, base64). Общий утилитный класс. */
public final class QrCodes {

    private QrCodes() {}

    public static String toDataUrl(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            // QR — вспомогательный элемент; при сбое возвращаем null, не ломая операцию
            return null;
        }
    }
}

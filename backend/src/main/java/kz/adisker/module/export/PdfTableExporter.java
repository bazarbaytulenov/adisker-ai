package kz.adisker.module.export;

import kz.adisker.common.exception.BusinessException;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Генерация PDF-таблиц на A4 (ТЗ п.13): книжная ориентация, кириллица (DejaVu),
 * колонтитул с датой формирования, пользователем и номером страницы.
 */
public final class PdfTableExporter {

    private PdfTableExporter() {}

    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 18f;
    private static final float FONT_SIZE = 9f;

    public static byte[] toPdf(String title, List<String> headers, List<List<String>> rows,
                               String generatedBy) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDType0Font font = loadFont(doc, "/fonts/DejaVuSans.ttf");
            PDType0Font fontBold = loadFont(doc, "/fonts/DejaVuSans-Bold.ttf");

            PDRectangle pageSize = PDRectangle.A4;
            float usableWidth = pageSize.getWidth() - 2 * MARGIN;
            int cols = Math.max(1, headers.size());
            float colWidth = usableWidth / cols;

            int rowsPerPage = (int) ((pageSize.getHeight() - 2 * MARGIN - 60) / ROW_HEIGHT);
            int totalRows = rows.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / rowsPerPage));

            int rowIndex = 0;
            for (int page = 0; page < totalPages; page++) {
                PDPage pdPage = new PDPage(pageSize);
                doc.addPage(pdPage);
                try (PDPageContentStream cs = new PDPageContentStream(doc, pdPage)) {
                    float y = pageSize.getHeight() - MARGIN;

                    // Заголовок (только на первой странице)
                    if (page == 0) {
                        cs.beginText();
                        cs.setFont(fontBold, 13);
                        cs.newLineAtOffset(MARGIN, y);
                        cs.showText(sanitize(title, font));
                        cs.endText();
                        y -= 28;
                    } else {
                        y -= 10;
                    }

                    // Заголовки колонок
                    y = drawRow(cs, fontBold, headers, MARGIN, y, colWidth, true, font);

                    // Данные страницы
                    for (int i = 0; i < rowsPerPage && rowIndex < totalRows; i++, rowIndex++) {
                        y = drawRow(cs, font, rows.get(rowIndex), MARGIN, y, colWidth, false, font);
                    }

                    // Колонтитул
                    cs.beginText();
                    cs.setFont(font, 8);
                    cs.newLineAtOffset(MARGIN, MARGIN - 15);
                    String footer = "Сформировано: " + LocalDate.now()
                            + (generatedBy != null ? "  |  Пользователь: " + generatedBy : "")
                            + "  |  Стр. " + (page + 1) + " из " + totalPages;
                    cs.showText(sanitize(footer, font));
                    cs.endText();
                }
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Не удалось сформировать PDF: " + e.getMessage());
        }
    }

    private static float drawRow(PDPageContentStream cs, PDType0Font font, List<String> cells,
                                 float x, float y, float colWidth, boolean bold, PDType0Font baseFont)
            throws Exception {
        cs.beginText();
        cs.setFont(font, FONT_SIZE);
        cs.newLineAtOffset(x, y);
        float offset = 0;
        for (String cell : cells) {
            String text = truncate(sanitize(cell, baseFont), colWidth, font);
            cs.showText(text);
            float shift = colWidth;
            cs.newLineAtOffset(shift, 0);
            offset += shift;
        }
        cs.endText();
        return y - ROW_HEIGHT;
    }

    /** Обрезает текст, чтобы влезал в ширину колонки. */
    private static String truncate(String text, float maxWidth, PDType0Font font) throws Exception {
        if (text.isEmpty()) return text;
        float limit = maxWidth - 4;
        while (text.length() > 1
                && font.getStringWidth(text) / 1000 * FONT_SIZE > limit) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    /** Заменяет символы, отсутствующие в шрифте, чтобы не падать при рендеринге. */
    private static String sanitize(String text, PDType0Font font) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            try {
                font.encode(String.valueOf(ch));
                sb.append(ch);
            } catch (Exception ex) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static PDType0Font loadFont(PDDocument doc, String resourcePath) throws Exception {
        try (InputStream is = PdfTableExporter.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new BusinessException("Шрифт не найден: " + resourcePath);
            return PDType0Font.load(doc, is);
        }
    }
}

package kz.adisker.module.export;

import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Универсальная генерация DOCX (ТЗ п.13, 20.3): протоколы, режим дня,
 * расписание, планы. Редактируемый текст.
 */
public final class WordExporter {

    private WordExporter() {}

    /** Документ с заголовком, необязательными абзацами и необязательной таблицей. */
    public static byte[] toDocx(String title, List<String> paragraphs,
                                List<String> tableHeaders, List<List<String>> tableRows) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Заголовок
            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleP.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(15);
            titleRun.setText(title != null ? title : "");

            // Абзацы
            if (paragraphs != null) {
                for (String p : paragraphs) {
                    XWPFParagraph para = doc.createParagraph();
                    XWPFRun run = para.createRun();
                    run.setFontSize(11);
                    run.setText(p != null ? p : "");
                }
            }

            // Таблица
            if (tableHeaders != null && !tableHeaders.isEmpty()) {
                XWPFTable table = doc.createTable();
                XWPFTableRow head = table.getRow(0);
                for (int c = 0; c < tableHeaders.size(); c++) {
                    XWPFTableCell cell = c == 0 ? head.getCell(0) : head.addNewTableCell();
                    setCellText(cell, tableHeaders.get(c), true);
                }
                if (tableRows != null) {
                    for (List<String> row : tableRows) {
                        XWPFTableRow tr = table.createRow();
                        for (int c = 0; c < row.size(); c++) {
                            // createRow создаёт ячейки по числу колонок первой строки
                            XWPFTableCell cell = c < tr.getTableCells().size()
                                    ? tr.getCell(c) : tr.addNewTableCell();
                            setCellText(cell, row.get(c), false);
                        }
                    }
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new kz.adisker.common.exception.BusinessException(
                    "Не удалось сформировать Word: " + e.getMessage());
        }
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun run = p.createRun();
        run.setBold(bold);
        run.setText(text != null ? text : "");
    }
}

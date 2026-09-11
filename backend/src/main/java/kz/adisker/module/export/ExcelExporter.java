package kz.adisker.module.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Универсальная генерация XLSX (ТЗ п.13): табели, своды, фонды, МТБ, платежи.
 */
public final class ExcelExporter {

    private ExcelExporter() {}

    /**
     * @param title    заголовок листа
     * @param headers  названия колонок
     * @param rows     строки (каждая — список значений, приводимых к строке)
     */
    public static byte[] toXlsx(String title, List<String> headers, List<List<Object>> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(safeSheetName(title));

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headFont = wb.createFont();
            headFont.setBold(true);
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setBorderBottom(BorderStyle.THIN);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            int r = 0;
            // Заголовок документа
            Row titleRow = sheet.createRow(r++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            if (!headers.isEmpty()) {
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        0, 0, 0, headers.size() - 1));
            }
            r++; // пустая строка

            // Заголовки колонок
            Row headerRow = sheet.createRow(r++);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headStyle);
            }

            // Данные
            for (List<Object> row : rows) {
                Row dataRow = sheet.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    Cell cell = dataRow.createCell(c);
                    Object v = row.get(c);
                    if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(v != null ? v.toString() : "");
                    }
                }
            }

            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new kz.adisker.common.exception.BusinessException(
                    "Не удалось сформировать Excel: " + e.getMessage());
        }
    }

    private static String safeSheetName(String name) {
        String s = name != null ? name.replaceAll("[\\\\/?*\\[\\]:]", " ") : "Лист";
        return s.length() > 31 ? s.substring(0, 31) : s;
    }
}

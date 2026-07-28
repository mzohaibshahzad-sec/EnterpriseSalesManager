package com.ledger.salesmanager.service;

import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.Sale;
import com.ledger.salesmanager.model.SaleItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates the exportable reports required by the spec: Product,
 * Inventory, Sales and Profit reports in CSV / Excel / PDF, plus a
 * printable PDF invoice for each sale.
 */
public class ReportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ---------------------------------------------------------------
    // CSV
    // ---------------------------------------------------------------
    public void exportProductsCsv(List<Product> products, Path outFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Name,SKU,Category,Brand,Wholesale Price,Selling Price,Total Qty,Sold Qty,Available Qty,Profit/Item\n");
        for (Product p : products) {
            sb.append(csv(p.getName())).append(',')
                    .append(csv(p.getSku())).append(',')
                    .append(csv(p.getCategoryName())).append(',')
                    .append(csv(p.getBrand())).append(',')
                    .append(p.getWholesalePrice()).append(',')
                    .append(p.getSellingPrice()).append(',')
                    .append(p.getTotalQuantity()).append(',')
                    .append(p.getSoldQuantity()).append(',')
                    .append(p.getAvailableQuantity()).append(',')
                    .append(p.getProfitPerItem()).append('\n');
        }
        Files.writeString(outFile, sb.toString());
    }

    public void exportSalesCsv(List<Sale> sales, Path outFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice,Date,Salesperson,Customer,Subtotal,Discount,Total,Profit,Payment Method,Status\n");
        for (Sale s : sales) {
            sb.append(csv(s.getInvoiceNumber())).append(',')
                    .append(s.getSaleDateTime() != null ? s.getSaleDateTime().format(DT) : "").append(',')
                    .append(csv(s.getSalespersonName())).append(',')
                    .append(csv(s.getCustomerName())).append(',')
                    .append(s.getSubtotal()).append(',')
                    .append(s.getDiscount()).append(',')
                    .append(s.getTotalAmount()).append(',')
                    .append(s.getTotalProfit()).append(',')
                    .append(s.getPaymentMethod()).append(',')
                    .append(s.getPaymentStatus()).append('\n');
        }
        Files.writeString(outFile, sb.toString());
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") ? "\"" + escaped + "\"" : escaped;
    }

    // ---------------------------------------------------------------
    // Excel (Apache POI)
    // ---------------------------------------------------------------
    public void exportProductsExcel(List<Product> products, Path outFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Products");
            CellStyle headerStyle = headerStyle(wb);

            String[] headers = {"Name", "SKU", "Category", "Brand", "Wholesale Price", "Selling Price",
                    "Total Qty", "Sold Qty", "Available Qty", "Profit/Item"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int r = 1;
            for (Product p : products) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(p.getName());
                row.createCell(1).setCellValue(nullToEmpty(p.getSku()));
                row.createCell(2).setCellValue(nullToEmpty(p.getCategoryName()));
                row.createCell(3).setCellValue(nullToEmpty(p.getBrand()));
                row.createCell(4).setCellValue(p.getWholesalePrice().doubleValue());
                row.createCell(5).setCellValue(p.getSellingPrice().doubleValue());
                row.createCell(6).setCellValue(p.getTotalQuantity());
                row.createCell(7).setCellValue(p.getSoldQuantity());
                row.createCell(8).setCellValue(p.getAvailableQuantity());
                row.createCell(9).setCellValue(p.getProfitPerItem().doubleValue());
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
                wb.write(fos);
            }
        }
    }

    public void exportSalesExcel(List<Sale> sales, Path outFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sales");
            CellStyle headerStyle = headerStyle(wb);

            String[] headers = {"Invoice", "Date", "Salesperson", "Customer", "Subtotal", "Discount",
                    "Total", "Profit", "Payment Method", "Status"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int r = 1;
            for (Sale s : sales) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.getInvoiceNumber());
                row.createCell(1).setCellValue(s.getSaleDateTime() != null ? s.getSaleDateTime().format(DT) : "");
                row.createCell(2).setCellValue(nullToEmpty(s.getSalespersonName()));
                row.createCell(3).setCellValue(nullToEmpty(s.getCustomerName()));
                row.createCell(4).setCellValue(s.getSubtotal().doubleValue());
                row.createCell(5).setCellValue(s.getDiscount().doubleValue());
                row.createCell(6).setCellValue(s.getTotalAmount().doubleValue());
                row.createCell(7).setCellValue(s.getTotalProfit().doubleValue());
                row.createCell(8).setCellValue(s.getPaymentMethod().name());
                row.createCell(9).setCellValue(s.getPaymentStatus().name());
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
                wb.write(fos);
            }
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    // ---------------------------------------------------------------
    // PDF (OpenPDF)
    // ---------------------------------------------------------------
    public void exportProductsPdf(List<Product> products, String storeName, Path outFile) throws IOException, DocumentException {
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 40, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(outFile.toFile()));
        doc.open();

        addTitle(doc, storeName, "Product Report");

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Name", "SKU", "Category", "Selling Price", "Total Qty", "Available Qty", "Profit/Item");
        for (Product p : products) {
            table.addCell(cell(p.getName()));
            table.addCell(cell(nullToEmpty(p.getSku())));
            table.addCell(cell(nullToEmpty(p.getCategoryName())));
            table.addCell(cell(p.getSellingPrice().toString()));
            table.addCell(cell(String.valueOf(p.getTotalQuantity())));
            table.addCell(cell(String.valueOf(p.getAvailableQuantity())));
            table.addCell(cell(p.getProfitPerItem().toString()));
        }
        doc.add(table);
        doc.close();
    }

    public void exportSalesPdf(List<Sale> sales, String storeName, Path outFile) throws IOException, DocumentException {
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 40, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(outFile.toFile()));
        doc.open();

        addTitle(doc, storeName, "Sales Report");

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Invoice", "Date", "Salesperson", "Total", "Profit", "Method", "Status");

        BigDecimal totalRevenue = BigDecimal.ZERO, totalProfit = BigDecimal.ZERO;
        for (Sale s : sales) {
            table.addCell(cell(s.getInvoiceNumber()));
            table.addCell(cell(s.getSaleDateTime() != null ? s.getSaleDateTime().format(DT) : ""));
            table.addCell(cell(nullToEmpty(s.getSalespersonName())));
            table.addCell(cell(s.getTotalAmount().toString()));
            table.addCell(cell(s.getTotalProfit().toString()));
            table.addCell(cell(s.getPaymentMethod().name()));
            table.addCell(cell(s.getPaymentStatus().name()));
            totalRevenue = totalRevenue.add(s.getTotalAmount());
            totalProfit = totalProfit.add(s.getTotalProfit());
        }
        doc.add(table);

        Paragraph summary = new Paragraph(
                "\nTotal Revenue: " + totalRevenue + "     Total Profit: " + totalProfit,
                new Font(Font.HELVETICA, 12, Font.BOLD));
        doc.add(summary);
        doc.close();
    }

    /** Printable invoice for a single completed sale. */
    public void generateInvoicePdf(Sale sale, List<SaleItem> items, String storeName, String storeAddress, Path outFile)
            throws IOException, DocumentException {
        Document doc = new Document(PageSize.A5, 30, 30, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(outFile.toFile()));
        doc.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10);

        doc.add(new Paragraph(storeName != null ? storeName : "Store", titleFont));
        if (storeAddress != null) doc.add(new Paragraph(storeAddress, normal));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Invoice: " + sale.getInvoiceNumber(), normal));
        doc.add(new Paragraph("Date: " + (sale.getSaleDateTime() != null ? sale.getSaleDateTime().format(DT) : ""), normal));
        doc.add(new Paragraph("Salesperson: " + sale.getSalespersonName(), normal));
        if (sale.getCustomerName() != null && !sale.getCustomerName().isBlank()) {
            doc.add(new Paragraph("Customer: " + sale.getCustomerName(), normal));
        }
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        addHeaderRow(table, "Item", "Qty", "Unit Price", "Line Total");
        for (SaleItem item : items) {
            table.addCell(cell(item.getProductName()));
            table.addCell(cell(String.valueOf(item.getQuantity())));
            table.addCell(cell(item.getUnitPrice().toString()));
            table.addCell(cell(item.getLineTotal().toString()));
        }
        doc.add(table);

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Subtotal: " + sale.getSubtotal(), normal));
        doc.add(new Paragraph("Discount: " + sale.getDiscount(), normal));
        doc.add(new Paragraph("Total: " + sale.getTotalAmount(), new Font(Font.HELVETICA, 13, Font.BOLD)));
        doc.add(new Paragraph("Payment: " + sale.getPaymentMethod() + " (" + sale.getPaymentStatus() + ")", normal));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Thank you for your business!", normal));

        doc.close();
    }

    private void addTitle(Document doc, String storeName, String reportTitle) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font subFont = new Font(Font.HELVETICA, 11);
        doc.add(new Paragraph(storeName != null ? storeName : "Store", titleFont));
        doc.add(new Paragraph(reportTitle, subFont));
        doc.add(new Paragraph(" "));
    }

    private void addHeaderRow(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD)));
            cell.setBackgroundColor(new Color(27, 36, 64));
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, new Font(Font.HELVETICA, 9)));
        cell.setPadding(5);
        return cell;
    }
}
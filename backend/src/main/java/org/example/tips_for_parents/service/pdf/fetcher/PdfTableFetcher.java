package org.example.tips_for_parents.service.pdf.fetcher;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.tips_for_parents.model.pdf.ExtractedPdfTable;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;
import org.springframework.stereotype.Component;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class PdfTableFetcher {

    public Optional<ExtractedPdfTable> find(byte[] pdf, Predicate<String> pageFilter,
                                            Predicate<List<List<String>>> tableFilter) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            ObjectExtractor extractor = new ObjectExtractor(doc);
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String raw = stripper.getText(doc);
                if (!pageFilter.test(normalize(raw))) continue;

                Page page = extractor.extract(p);
                for (Table t : new SpreadsheetExtractionAlgorithm().extract(page)) {
                    List<List<String>> rows = toRows(t);
                    if (tableFilter.test(rows)) return Optional.of(new ExtractedPdfTable(p, rows, extractNote(raw)));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<ExtractedPdfTable> feedingScheme(byte[] pdf) throws IOException {
        return find(pdf,
                t -> t.contains("Примерная схема введения продуктов") && t.contains("Возраст"),
                rows -> head(rows, 3).stream().anyMatch(r -> r.stream().anyMatch(c -> low(c).contains("возраст, мес"))));
    }

    public Optional<ExtractedPdfTable> norms(byte[] pdf) throws IOException {
        return find(pdf,
                t -> t.contains("Среднесуточные нормы") && t.contains("Энергия"),
                rows -> head(rows, 2).stream().anyMatch(r -> r.stream().anyMatch(c -> low(c).startsWith("энергия"))));
    }

    public Optional<ExtractedPdfTable> nutrients(byte[] pdf) throws IOException {
        return find(pdf,
                t -> t.contains("Пищевая ценность") && t.contains("прикорма"),
                rows -> head(rows, 2).stream().anyMatch(r -> r.size() >= 4
                        && low(r.get(0)).startsWith("продукт") && r.stream().anyMatch(c -> low(c).contains("белк"))));
    }

    public Optional<ExtractedPdfTable> weightGain(byte[] pdf) throws IOException {
        return find(pdf,
                t -> t.contains("прибавка"),
                rows -> head(rows, 2).stream().anyMatch(r -> r.stream().anyMatch(c -> low(c).contains("прибавка массы"))
                        && r.stream().anyMatch(c -> low(c).contains("прибавка роста"))));
    }

    public Optional<ExtractedPdfTable> formulaDilution(byte[] pdf) throws IOException {
        return find(pdf,
                t -> t.contains("Разведение смесей"),
                rows -> head(rows, 1).stream().anyMatch(r -> low(r.get(0)).startsWith("разведение смесей")));
    }

    public Optional<Integer> findPage(byte[] pdf, Predicate<String> pageFilter) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                if (pageFilter.test(normalize(stripper.getText(doc)))) return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public String flatPageText(byte[] pdf, int pageNumber) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            return normalize(stripper.getText(doc));
        }
    }

    private static List<List<String>> head(List<List<String>> rows, int n) {
        return rows.subList(0, Math.min(n, rows.size()));
    }

    private static String low(String s) {
        return NumberParsingUtils.clean(s).toLowerCase();
    }

    private static List<List<String>> toRows(Table t) {
        List<List<String>> rows = new ArrayList<>();
        for (List<RectangularTextContainer> row : t.getRows()) {
            List<String> cells = new ArrayList<>();
            for (RectangularTextContainer c : row) cells.add(c.getText());
            rows.add(cells);
        }
        return rows;
    }

    static String normalize(String s) {
        return s.replace("\u00AD", "").replace('\u00A0', ' ').replaceAll("\\s+", " ");
    }

    static String extractNote(String pageText) {
        int i = pageText.indexOf("Примечание");
        if (i < 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : pageText.substring(i).split("\\R")) {
            sb.append(line.strip()).append(' ');
            if (sb.indexOf("*") >= 0 && line.strip().endsWith(".")) break;
            if (sb.length() > 700) break;
        }
        return sb.toString().strip();
    }
}

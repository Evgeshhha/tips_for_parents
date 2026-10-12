package org.example.tips_for_parents.service.pdf.extractor;

import lombok.RequiredArgsConstructor;
import org.example.tips_for_parents.model.pdf.MilkComposition;
import org.example.tips_for_parents.model.pdf.MilkFeedingData;
import org.example.tips_for_parents.model.pdf.MilkVolumeLimit;
import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;
import org.example.tips_for_parents.service.pdf.fetcher.PdfRulesFetcher;
import org.example.tips_for_parents.service.pdf.fetcher.PdfTableFetcher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PdfMilkExtractor {

    private static final String NUM = "(\\d+(?:[.,]\\d+)?)";
    private static final Pattern BREAST = Pattern.compile("(?iu)сцеженное\\s+грудное\\s+молоко\\s*\\(состав на 100 мл:\\s*"
            + "энергоценность\\s+" + NUM + "\\s*ккал,\\s*белок\\s+" + NUM + "\\s*г,\\s*жир\\s+" + NUM
            + "\\s*г,\\s*углеводы\\s+" + NUM + "\\s*г\\)");
    private static final Pattern VOLUMES = Pattern.compile("(?iu)в возрасте\\s+(\\d+)\\s*мес\\s+не должен превышать\\s+(\\d+)\\s*мл/сут,\\s*"
            + "(\\d+)\\s*мес\\s*[—–-]\\s*(\\d+)\\s*мл/сут,\\s*а после\\s+(\\d+)\\s*мес\\s*[—–-]\\s*(\\d+)\\s*мл/сут");

    private final PdfTableFetcher tables;
    private final PdfRulesFetcher text;

    public MilkFeedingData extract(byte[] pdf) throws IOException {
        List<MilkComposition> comps = new ArrayList<>();
        List<MilkVolumeLimit> vols = new ArrayList<>();

        var t76 = tables.formulaDilution(pdf);
        if (t76.isPresent()) {
            var rows = t76.get().rows();
            for (int i = 1; i < rows.size(); i++) {                       // первая строка данных — стандартное разведение смеси 1
                var r = rows.get(i);
                if (r.size() < 5 || !NumberParsingUtils.clean(r.get(0)).toLowerCase().startsWith("смесь")) continue;
                var e = NumberParsingUtils.parseRange(r.get(1));
                var p = NumberParsingUtils.parseRange(r.get(2));
                var f = NumberParsingUtils.parseRange(r.get(3));
                var c = NumberParsingUtils.parseRange(r.get(4));
                if (e == null || p == null || f == null || c == null) continue;
                comps.add(new MilkComposition("formula", "Адаптированная смесь 1, стандартное разведение (на 100 мл)",
                        e.lo(), p.lo(), f.lo(), c.lo(), NumberParsingUtils.clean(r.get(0))));
                break;
            }
            // примечание идёт через всю ширину страницы — читаем текст страницы целиком, а не по колонкам
            Matcher m = BREAST.matcher(tables.flatPageText(pdf, t76.get().pageNumber()));
            if (m.find())
                comps.add(new MilkComposition("breast", "Сцеженное грудное молоко (на 100 мл)", NumberParsingUtils.d(m.group(1)),
                        NumberParsingUtils.d(m.group(2)), NumberParsingUtils.d(m.group(3)), NumberParsingUtils.d(m.group(4)), m.group(0)));
        }

        Optional<Integer> page = tables.findPage(pdf, t -> t.contains("850 мл/сут"));
        if (page.isPresent()) {
            String pageText = text.pageText(pdf, page.get());
            Matcher m = VOLUMES.matcher(pageText);
            if (m.find()) {
                String sentence = PdfRulesFetcher.sentenceAround(pageText, "не должен превышать");
                String s = sentence != null ? sentence : m.group(0);
                int after = Integer.parseInt(m.group(5));
                vols.add(new MilkVolumeLimit(m.group(1) + " мес", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)), s));
                vols.add(new MilkVolumeLimit(m.group(3) + " мес", Integer.parseInt(m.group(3)), Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(4)), s));
                // «после 5 мес» читаем как «с 5 мес и старше» (для самого возраста 5 мес граница в тексте нечёткая)
                vols.add(new MilkVolumeLimit("после " + after + " мес", after, null, Integer.parseInt(m.group(6)), s));
            }
        }
        return new MilkFeedingData(comps, vols);
    }
}

package org.example.tips_for_parents.service.card;

import org.example.tips_for_parents.service.pdf.util.NumberParsingUtils;

/** Ключ продукта для персонализации и сопоставления названий: название строки таблицы 5.1 в нижнем регистре. */
public final class ProductKeys {
    private ProductKeys() {
    }

    public static String of(String label) {
        return NumberParsingUtils.clean(label).toLowerCase();
    }
}

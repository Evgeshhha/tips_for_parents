package org.example.tips_for_parents.service.source;

import org.example.tips_for_parents.model.report.SourceImportReport;

/** Загрузка и разбор одного источника; ошибки превращаются в отчёт со статусом ERROR в SourceImportService. */
@FunctionalInterface
interface SourceTask {
    SourceImportReport run() throws Exception;
}

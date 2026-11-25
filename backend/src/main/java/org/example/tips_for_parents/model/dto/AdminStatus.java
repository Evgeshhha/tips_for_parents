package org.example.tips_for_parents.model.dto;

import org.example.tips_for_parents.model.report.RefreshReport;

import java.util.Map;

public record AdminStatus(Map<String, Map<String, Long>> factsByTypeAndStatus, RefreshReport lastRefresh) {}

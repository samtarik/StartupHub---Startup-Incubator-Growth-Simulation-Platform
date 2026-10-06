package com.startuphub.dto;

import com.startuphub.model.Revenue;
import com.startuphub.model.RevenueModel;
import java.time.LocalDate;

/** model/details are null for manually entered revenue and filled for calculator revenue. */
public record RevenueDto(Long id, double amount, String source, LocalDate recordedOn,
                         RevenueModel model, String details) {
    public static RevenueDto from(Revenue r) {
        return new RevenueDto(r.getId(), r.getAmount(), r.getSource(), r.getRecordedOn(),
                r.getRevenueModel(), r.getCalculationDetails());
    }
}

package com.group3.vitamins.bidding.bidsummary.application.command;

public record UpdateBidNoticeSummaryCommand(
        Long summaryId,
        SummaryPatchField overviewSummary,
        SummaryPatchField amountSummary,
        SummaryPatchField scheduleSummary,
        SummaryPatchField qualificationSummary,
        SummaryPatchField taskSummary,
        SummaryPatchField riskSummary,
        String userId,
        String role
) {
    public UpdateBidNoticeSummaryCommand {
        overviewSummary = normalize(overviewSummary);
        amountSummary = normalize(amountSummary);
        scheduleSummary = normalize(scheduleSummary);
        qualificationSummary = normalize(qualificationSummary);
        taskSummary = normalize(taskSummary);
        riskSummary = normalize(riskSummary);
    }

    public boolean hasChanges() {
        return overviewSummary.present()
                || amountSummary.present()
                || scheduleSummary.present()
                || qualificationSummary.present()
                || taskSummary.present()
                || riskSummary.present();
    }

    private static SummaryPatchField normalize(SummaryPatchField field) {
        return field == null ? SummaryPatchField.absent() : field;
    }
}

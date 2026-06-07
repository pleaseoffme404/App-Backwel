package com.backwell.api_service.modules.inventory.dto;

public record InventoryDeltaRequest(
        int availableDelta,
        int reservedDelta,
        int redundancyDelta,
        int physicalDelta
) {
    public boolean isValidMovement() {
        boolean isIncomeOrLoss = physicalDelta == (availableDelta + reservedDelta + redundancyDelta);
        boolean isTransfer = 0 == (physicalDelta + availableDelta + reservedDelta + redundancyDelta);
        return isIncomeOrLoss != isTransfer;
    }
}

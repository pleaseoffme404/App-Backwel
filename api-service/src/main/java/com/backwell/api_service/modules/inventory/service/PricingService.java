package com.backwell.api_service.modules.inventory.service;

import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.repo.PriceCalculationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final UUIDService uuidService;
    private final PriceCalculationHistoryRepository priceCalculationHistoryRepository;
    private final PriceStreamProcessor priceStreamProcessor;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private static final String CRON_RECALCULATION = "0 5 */4 * * *";
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);


    @Scheduled(cron = CRON_RECALCULATION, zone = "UTC")
    public void scheduledRecalculate() {
        log.info("[Scheduler] Ejecución automática disparada por Cron.");
        try {
            executeRecalculationWorkflow();
        } catch (SystemException e) {
            log.warn("[Scheduler] Ejecución automática omitida: {}", e.getMessage());
        }
    }

    /**
     * Punto de entrada público expuesto para peticiones manuales de clientes (API REST / On-Demand).
     */
    public void manualRecalculate() {
        log.info("[Scheduler] Solicitud manual de sincronización inmediata recibida.");
        executeRecalculationWorkflow();
    }

    private void executeRecalculationWorkflow() {
        if (!isProcessing.compareAndSet(false, true)) {
            throw new SystemException("La tarea de recálculo de precios ya se encuentra en ejecución en este momento.");
        }

        final UUID transactionId = uuidService.next();
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting price recalculation task for transaction {}", transactionId);
            processPriceRecalculation(transactionId);

            priceStreamProcessor.streamAndIndexPrices(transactionId);
            log.info("Finished price recalculation task for transaction {} in {}ms",
                    transactionId, (System.currentTimeMillis() - startTime));
        } finally {
            isProcessing.set(false);
        }
    }

    /**
     * Calcula de manera exacta cuándo será la próxima ejecución programada en base al huso horario UTC.
     * @return Un Optional con el ZonedDateTime de la próxima ejecución o vacío si la expresión es inválida.
     */
    public Optional<ZonedDateTime> getNextScheduledExecutionTime() {
        CronExpression cron = CronExpression.parse(CRON_RECALCULATION);
        // Calculamos el siguiente instante a partir del momento exacto del presente (UTC)
        ZonedDateTime nextExecution = cron.next(ZonedDateTime.now(java.time.ZoneOffset.UTC));
        return Optional.ofNullable(nextExecution);
    }

    /**
     * Permite conocer externamente si el proceso está corriendo actualmente.
     */
    public boolean isCurrentlyProcessing() {
        return isProcessing.get();
    }


    /**
     * Calls a procedure to resolve discounts and price changes*/
    private void processPriceRecalculation(UUID transactionId) {
        if (transactionId == null) return;
        String sql = "CALL pr_process_price_recalculation(:transactionId)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("transactionId", transactionId);
        jdbcTemplate.update(sql, params);
    }
}

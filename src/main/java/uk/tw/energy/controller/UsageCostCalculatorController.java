package uk.tw.energy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usage-cost")
public class UsageCostCalculatorController {

    private final AccountService accountService;
    private final MeterReadingService meterReadingService;
    private final PricePlanService pricePlanService;

    public UsageCostCalculatorController(AccountService accountService, MeterReadingService meterReadingService, PricePlanService pricePlanService){
        this.accountService = accountService;
        this.meterReadingService = meterReadingService;
        this.pricePlanService = pricePlanService;
    }


    @GetMapping("/{smartMeterId}")
    public ResponseEntity<Object> getUsageCostOfAMeterForParticularDuration(@PathVariable String smartMeterId, @RequestParam(value = "duration") int duration){

        if(duration < 0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("duration cannot be less than 0");
        }
        Optional<String> pricePlanId = Optional.ofNullable(accountService.getPricePlanIdForSmartMeterId(smartMeterId));
        if(!pricePlanId.isPresent()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No Plan is attached to the meter");
        }

        Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);

        if(!readings.isPresent()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No reading are recorded for the meter");
        }

        BigDecimal cost = pricePlanService.calculateCostForPricePlanId(readings.get().stream()
                .filter(reading -> ChronoUnit.DAYS.between(reading.getTime(), Instant.now()) <= duration)
                .collect(Collectors.toList()),pricePlanId.get());

      return ResponseEntity.status(HttpStatus.OK).body(cost);
    }

}

package com.libraryms.loan;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.loans")
public class LoanProperties {

    private int durationDays = 14;
    private BigDecimal finePerDay = new BigDecimal("0.50");
    private int maxOpenLoans = 5;

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public BigDecimal getFinePerDay() {
        return finePerDay;
    }

    public void setFinePerDay(BigDecimal finePerDay) {
        this.finePerDay = finePerDay;
    }

    public int getMaxOpenLoans() {
        return maxOpenLoans;
    }

    public void setMaxOpenLoans(int maxOpenLoans) {
        this.maxOpenLoans = maxOpenLoans;
    }
}

package com.example.record.domain;

import com.example.domain.Currency;
import com.example.domain.IRecord;

import java.math.BigDecimal;

public class TempRecord implements IRecord {
    String sign;
    Currency currency;
    BigDecimal amount;

    public TempRecord(String sign, Currency currency, BigDecimal amount) {
        this.sign = sign;
        this.currency = currency;
        this.amount = amount;
    }

    public String getSign() {
        return sign;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }



    public void setSign(String sign) {
        this.sign = sign;
    }

    public Integer getCurrencyCode() {
        return currency.getCode();
    }

    public void setCurrencyCode(Integer currencyCode) {
        this.currency.setCode(currencyCode);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

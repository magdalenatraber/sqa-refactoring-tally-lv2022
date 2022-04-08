package com.example.domain;

import java.math.BigDecimal;

public interface IRecord {

    public String getSign();

    public Currency getCurrency();

    public BigDecimal getAmount();
}

package com.example.confirmationletter;

import com.example.dao.CurrencyDao;
import com.example.domain.Currency;
import com.example.domain.*;
import com.example.domain.Record;
import com.example.record.domain.TempRecord;
import com.example.record.service.impl.Constants;
import com.example.record.service.impl.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class ConfirmationLetterTally {

    private CurrencyDao currencyDao;

    public Map<String, BigDecimal> calculateAmounts(
            Client client,
            List<Record> records,
            CurrencyDao currencyDao,
            List<com.example.record.domain.FaultRecord> faultyRecords,
            List<TempRecord> faultyAccountNumberRecordList,
            List<TempRecord> sansDuplicateFaultRecordsList,
            Map<Integer, BatchTotal> batchTotals
    ) {
        Map<String, BigDecimal> result = calculateRetrieveAmounts(records, faultyRecords,
                client, faultyAccountNumberRecordList, sansDuplicateFaultRecordsList);
        result.put("CreditBatchTotal", creditBatchTotal(
                batchTotals.values(),
                client.getAmountDivider(),
                BatchTotal::getCreditValue));
        result.put("DebitBatchTotal", creditBatchTotal(
                batchTotals.values(),
                client.getAmountDivider(),
                BatchTotal::getCreditCounterValueForDebit));
        return result;
    }

    class Tally{
        BigDecimal creditFL = new BigDecimal(0);
        BigDecimal creditUSD = new BigDecimal(0);
        BigDecimal creditEUR = new BigDecimal(0);

        BigDecimal debitFL = new BigDecimal(0);
        BigDecimal debitUSD = new BigDecimal(0);
        BigDecimal debitEUR = new BigDecimal(0);

        void addTempRecord(IRecord tempRecord){
            if (tempRecord.getCurrency().getCode().equals(Constants.FL_CURRENCY_CODE)
                    || tempRecord.getCurrency().getCode().equals(Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK)) {
                if (tempRecord.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                    debitFL = tempRecord.getAmount().add(debitFL);
                } else {
                    creditFL = tempRecord.getAmount().add(creditFL);
                }
            }
            if (tempRecord.getCurrency().getCode().equals(Constants.USD_CURRENCY_CODE)) {
                if (tempRecord.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                    debitUSD = tempRecord.getAmount().add(debitUSD);
                } else {
                    creditUSD = tempRecord.getAmount().add(creditUSD);
                }
            }
            if (tempRecord.getCurrency().getCode().equals(Constants.EUR_CURRENCY_CODE)) {
                if (tempRecord.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                    debitEUR = tempRecord.getAmount().add(debitEUR);
                } else {
                    creditEUR = tempRecord.getAmount().add(creditEUR);
                }
            }
        }
    }
    // Calculate sum amount from faultyAccountnumber list
    private Map<String, BigDecimal> calculateAmountsFaultyAccountNumber(
            List<TempRecord> faultyAccountNumberRecordList, Client client) {

        Tally tally = new Tally();
        for (TempRecord faultyAccountNumberRecord : faultyAccountNumberRecordList) {
            // FL
            fixSignAndCurrencyCode(client, faultyAccountNumberRecord);

            tally.addTempRecord(faultyAccountNumberRecord);
        }
            Map<String, BigDecimal> result = new HashMap<>();
            result.put("FaultyAccDebitFL", tally.debitFL);
            result.put("FaultyAccDebitUSD", tally.debitUSD);
            result.put("FaultyAccDebitEUR", tally.debitEUR);
            result.put("FaultyAccCreditFL", tally.creditFL);
            result.put("FaultyAccCreditUSD", tally.creditUSD);
            result.put("FaultyAccCreditEUR", tally.creditEUR);

            return result;
    }

    private void fixSignAndCurrencyCode(Client client, TempRecord faultyAccountNumberRecord) {
       String sign = faultyAccountNumberRecord.getSign();
        if (sign == null || StringUtils.isBlank(sign)) {
            faultyAccountNumberRecord.setSign(client.getCreditDebit());
        }

        if (faultyAccountNumberRecord.getCurrencyCode() == null) {
            String currencyId = currencyDao.retrieveCurrencyDefault(client.getProfile());
            Currency currency = currencyDao.retrieveCurrencyOnId(new Integer(currencyId));
            faultyAccountNumberRecord.setCurrencyCode(currency.getCode());
        }
    }

    private Map<String, BigDecimal> calculateRetrieveAmounts(
            List<Record> records,
            List<com.example.record.domain.FaultRecord> faultyRecords,
            Client client,
            List<TempRecord> faultyAccountNumberRecordList,
            List<TempRecord> sansDuplicateFaultRecordsList) {

        Map<String, BigDecimal> retrievedAmounts = new HashMap<String, BigDecimal>();

        BigDecimal recordAmountFL = new BigDecimal(0);
        BigDecimal recordAmountUSD = new BigDecimal(0);
        BigDecimal recordAmountEUR = new BigDecimal(0);

        BigDecimal recordAmountDebitFL = new BigDecimal(0);
        BigDecimal recordAmountDebitEUR = new BigDecimal(0);
        BigDecimal recordAmountDebitUSD = new BigDecimal(0);

        BigDecimal recordAmountCreditFL = new BigDecimal(0);
        BigDecimal recordAmountCreditEUR = new BigDecimal(0);
        BigDecimal recordAmountCreditUSD = new BigDecimal(0);

        Tally amountSansTally = new Tally();

        BigDecimal totalDebitFL = new BigDecimal(0);
        BigDecimal totalDebitUSD = new BigDecimal(0);
        BigDecimal totalDebitEUR = new BigDecimal(0);

        BigDecimal totalCreditFL = new BigDecimal(0);
        BigDecimal totalCreditUSD = new BigDecimal(0);
        BigDecimal totalCreditEUR = new BigDecimal(0);

        Tally tally = new Tally();
        if (client.getCounterTransfer().equalsIgnoreCase(Constants.TRUE)) {
            for (Record record : records) {
                tally.addTempRecord(record);
            }
                retrievedAmounts.put(Constants.CURRENCY_EURO, recordAmountEUR);
                retrievedAmounts.put(Constants.CURRENCY_USD, recordAmountUSD);
                retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountFL);
            }

        // Not Balanced
        else {

            for (Record record : records) {
                if (record.getIsCounterTransferRecord().compareTo(new Integer(0)) == 0
                        && record.getFeeRecord().compareTo(new Integer(0)) == 0) {
                    if ((record.getCurrency().getCode().equals(
                            Constants.FL_CURRENCY_CODE) || record
                            .getCurrency().getCode().equals(
                                    Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK))) {
                        if (record.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                            recordAmountDebitFL = record.getAmount().add(
                                    recordAmountDebitFL);
                        }
                        if (record.getSign().equalsIgnoreCase(Constants.CREDIT)) {
                            recordAmountCreditFL = record.getAmount().add(
                                    recordAmountCreditFL);
                        }

                        if (record.getCurrency().getCode().equals(
                                Constants.EUR_CURRENCY_CODE)) {

                            if (record.getSign().equalsIgnoreCase(
                                    Constants.DEBIT)) {
                                recordAmountDebitEUR = record.getAmount().add(
                                        recordAmountDebitEUR);
                            }
                            if (record.getSign().equalsIgnoreCase(
                                    Constants.CREDIT)) {
                                recordAmountCreditEUR = record.getAmount().add(
                                        recordAmountCreditEUR);
                            }

                        }

                    }
                }

                if (record.getCurrency().getCode().equals(
                        Constants.USD_CURRENCY_CODE)) {

                    if (record.getSign().equalsIgnoreCase(Constants.DEBIT)) {
                        recordAmountDebitUSD = record.getAmount().add(
                                recordAmountDebitUSD);
                    }
                    if (record.getSign().equalsIgnoreCase(Constants.CREDIT)) {
                        recordAmountCreditUSD = record.getAmount().add(
                                recordAmountCreditUSD);
                    }

                }

            }
            // Sansduplicate
            for (TempRecord sansDupRec : sansDuplicateFaultRecordsList) {
            fixSignAndCurrencyCode(client,sansDupRec);
                    amountSansTally.addTempRecord(sansDupRec);
            }

            Map<String, BigDecimal> retrievedAccountNumberAmounts = calculateAmountsFaultyAccountNumber(
                    faultyAccountNumberRecordList, client);
            if (retrievedAccountNumberAmounts.get("FaultyAccDebitFL") != null) {
                totalDebitFL = recordAmountDebitFL.add(amountSansTally.debitFL)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccDebitFL"));
            } else {
                totalDebitFL = recordAmountDebitFL.add(amountSansTally.debitFL);
            }


            if (retrievedAccountNumberAmounts.get("FaultyAccCreditFL") != null){
                totalCreditFL = recordAmountCreditFL.add(amountSansTally.creditFL)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccCreditFL"));
            } else {
                totalCreditFL = recordAmountCreditFL.add(amountSansTally.creditFL);
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccDebitUSD") != null) {
                totalDebitUSD = recordAmountDebitUSD.add(amountSansTally.debitUSD)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccDebitUSD"));
            } else {
                totalDebitUSD = recordAmountDebitUSD.add(amountSansTally.debitUSD);
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditUSD") != null) {
                totalCreditUSD = recordAmountCreditUSD.add(amountSansTally.creditUSD)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccCreditUSD"));
            } else {
                totalCreditUSD = recordAmountCreditUSD.add(amountSansTally.creditUSD);
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccDebitEUR") != null) {
                totalDebitEUR = recordAmountDebitEUR.add(amountSansTally.debitEUR)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccDebitEUR"));
            } else{
                totalDebitEUR = recordAmountDebitEUR.add(amountSansTally.debitEUR);
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditEUR") != null){
                totalCreditEUR = recordAmountCreditEUR.add(amountSansTally.creditEUR)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccCreditEUR"));
            } else{
                totalCreditEUR = recordAmountCreditEUR.add(amountSansTally.creditEUR);
            }

            recordAmountFL = totalDebitFL.subtract(totalCreditFL).abs();
            recordAmountUSD = totalDebitUSD.subtract(totalCreditUSD).abs();
            recordAmountEUR = totalDebitEUR.subtract(totalCreditEUR).abs();

            retrievedAmounts.put(Constants.CURRENCY_EURO, recordAmountEUR);
            retrievedAmounts.put(Constants.CURRENCY_USD, recordAmountUSD);
            retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountFL);

        }

        return retrievedAmounts;
    }

    interface BatchValueAccessor {
        BigDecimal get(BatchTotal batchTotal);
        //  BigDecimal BatchTotal::getCreditValue()
    }

    BigDecimal creditBatchTotal(Collection<BatchTotal> batchTotals, BigDecimal amountDivider,
                                BatchValueAccessor value) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BatchTotal total : batchTotals) {
            sum = sum.add(value.get(total)); // total.getCreditValue()
        }
        sum = sum.divide(amountDivider);
        return sum;
    }
}
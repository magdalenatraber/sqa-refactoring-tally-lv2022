package com.example.confirmationletter;

import com.example.dao.CurrencyDao;
import com.example.domain.Currency;
import com.example.domain.*;
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

    // Calculate sum amount from faultyAccountnumber list
    private Map<String, BigDecimal> calculateAmountsFaultyAccountNumber(
            List<TempRecord> faultyAccountNumberRecordList, Client client) {
        Map<String, BigDecimal> retrievedAmountsFaultyAccountNumber = new HashMap<String, BigDecimal>();

        BigDecimal faultyAccRecordAmountCreditFL = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountCreditUSD = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountCreditEUR = new BigDecimal(0);

        BigDecimal faultyAccRecordAmountDebitFL = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountDebitUSD = new BigDecimal(0);
        BigDecimal faultyAccRecordAmountDebitEUR = new BigDecimal(0);

        for (TempRecord faultyAccountNumberRecord : faultyAccountNumberRecordList) {
            // FL
            if (StringUtils.isBlank(faultyAccountNumberRecord.getSign())) {
                faultyAccountNumberRecord.setSign(client.getCreditDebit());
            }

            if (faultyAccountNumberRecord.getCurrencyCode() == null) {
                String currencyId = currencyDao.retrieveCurrencyDefault(client.getProfile());
                Currency currency = currencyDao.retrieveCurrencyOnId(new Integer(currencyId));
                faultyAccountNumberRecord.setCurrencyCode(currency.getCode());
            }

            if (faultyAccountNumberRecord.getCurrencyCode().equals(
                    Constants.FL_CURRENCY_CODE)
                    || faultyAccountNumberRecord.getCurrencyCode().equals(
                    Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK)) {

                if (faultyAccountNumberRecord.getSign().equalsIgnoreCase(
                        Constants.DEBIT)) {
                    faultyAccRecordAmountDebitFL = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountDebitFL);
                } else {
                    faultyAccRecordAmountCreditFL = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountCreditFL);
                }
            }
            if (faultyAccountNumberRecord.getCurrencyCode().equals(
                    Constants.USD_CURRENCY_CODE)) {
                if (faultyAccountNumberRecord.getSign().equalsIgnoreCase(
                        Constants.DEBIT)) {
                    faultyAccRecordAmountDebitUSD = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountDebitUSD);
                } else {
                    faultyAccRecordAmountCreditUSD = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountCreditUSD);
                }
            }
            if (faultyAccountNumberRecord.getCurrencyCode().equals(
                    Constants.EUR_CURRENCY_CODE)) {
                if (faultyAccountNumberRecord.getSign().equalsIgnoreCase(
                        Constants.DEBIT)) {
                    faultyAccRecordAmountDebitEUR = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountDebitEUR);
                } else {
                    faultyAccRecordAmountCreditEUR = new BigDecimal(
                            faultyAccountNumberRecord.getAmount())
                            .add(faultyAccRecordAmountCreditEUR);
                }
            }

            retrievedAmountsFaultyAccountNumber.put("FaultyAccDebitFL",
                    faultyAccRecordAmountDebitFL);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccDebitUSD",
                    faultyAccRecordAmountDebitUSD);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccDebitEUR",
                    faultyAccRecordAmountDebitEUR);

            retrievedAmountsFaultyAccountNumber.put("FaultyAccCreditFL",
                    faultyAccRecordAmountCreditFL);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccCreditUSD",
                    faultyAccRecordAmountCreditUSD);
            retrievedAmountsFaultyAccountNumber.put("FaultyAccCreditEUR",
                    faultyAccRecordAmountCreditEUR);

        }
        return retrievedAmountsFaultyAccountNumber;
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

        BigDecimal amountSansDebitFL = new BigDecimal(0);
        BigDecimal amountSansDebitUSD = new BigDecimal(0);
        BigDecimal amountSansDebitEUR = new BigDecimal(0);

        BigDecimal amountSansCreditFL = new BigDecimal(0);
        BigDecimal amountSansCreditUSD = new BigDecimal(0);
        BigDecimal amountSansCreditEUR = new BigDecimal(0);

        BigDecimal totalDebitFL = new BigDecimal(0);
        BigDecimal totalDebitUSD = new BigDecimal(0);
        BigDecimal totalDebitEUR = new BigDecimal(0);

        BigDecimal totalCreditFL = new BigDecimal(0);
        BigDecimal totalCreditUSD = new BigDecimal(0);
        BigDecimal totalCreditEUR = new BigDecimal(0);

        if (client.getCounterTransfer().equalsIgnoreCase(Constants.TRUE)) {
            for (Record record : records) {
                if (record.getFeeRecord() != 1) {
                    if ((record.getCurrency().getCode().equals(
                            Constants.FL_CURRENCY_CODE) || record
                            .getCurrency().getCode().equals(
                                    Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK))
                            && record.getSign().equalsIgnoreCase(
                            Constants.DEBIT)) {
                        recordAmountFL = record.getAmount().add(
                                recordAmountFL);
                    }
                    if (record.getCurrency().getCode().equals(
                            Constants.EUR_CURRENCY_CODE)
                            && record.getSign().equalsIgnoreCase(
                            Constants.DEBIT)) {
                        recordAmountEUR = record.getAmount().add(
                                recordAmountEUR);
                    }
                    if (record.getCurrency().getCode().equals(
                            Constants.USD_CURRENCY_CODE)
                            && record.getSign().equalsIgnoreCase(
                            Constants.DEBIT)) {
                        recordAmountUSD = record.getAmount().add(
                                recordAmountUSD);
                    }
                }
                retrievedAmounts.put(Constants.CURRENCY_EURO, recordAmountEUR);
                retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountUSD);
                retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountFL);
            }
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
                Integer currencyCode = sansDupRec.getCurrencyCode();
                if (sansDupRec.getSign() == null) {
                    String sign = client.getCreditDebit();
                    sansDupRec.setSign(sign);
                }
                if (currencyCode == null) {
                    String currencyId = currencyDao
                            .retrieveCurrencyDefault(client.getProfile());
                    Currency currency = currencyDao
                            .retrieveCurrencyOnId(new Integer(currencyId));
                    sansDupRec.setCurrencyCode(currency.getCode());
                } else {

                    if (currencyCode.equals(Constants.FL_CURRENCY_CODE)
                            || currencyCode
                            .equals(Constants.FL_CURRENCY_CODE_FOR_WEIRD_BANK)) {

                        if (sansDupRec.getSign().equalsIgnoreCase(
                                Constants.DEBIT)) {
                            amountSansDebitFL = new BigDecimal(sansDupRec
                                    .getAmount()).add(amountSansDebitFL);
                        } else {
                            amountSansCreditFL = new BigDecimal(sansDupRec
                                    .getAmount()).add(amountSansCreditFL);
                        }
                    }
                    if (currencyCode.equals(Constants.USD_CURRENCY_CODE)) {
                        if (sansDupRec.getSign().equalsIgnoreCase(
                                Constants.DEBIT)) {
                            amountSansDebitUSD = new BigDecimal(sansDupRec
                                    .getAmount()).add(amountSansDebitUSD);
                        } else {
                            amountSansCreditUSD = new BigDecimal(sansDupRec
                                    .getAmount()).add(amountSansCreditUSD);
                        }
                    }
                    if (currencyCode.equals(Constants.EUR_CURRENCY_CODE)) {
                        if (sansDupRec.getSign().equalsIgnoreCase(
                                Constants.DEBIT)) {
                            amountSansDebitEUR = new BigDecimal(sansDupRec
                                    .getAmount()).add(amountSansDebitEUR);
                        } else {
                            amountSansCreditEUR = new BigDecimal(sansDupRec
                                    .getAmount()).add(amountSansCreditEUR);
                        }
                    }
                }

            }

            Map<String, BigDecimal> retrievedAccountNumberAmounts = calculateAmountsFaultyAccountNumber(
                    faultyAccountNumberRecordList, client);
            if (retrievedAccountNumberAmounts.get("FaultyAccDebitFL") != null
                    && amountSansDebitFL != null) {
                totalDebitFL = recordAmountDebitFL.add(amountSansDebitFL)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccDebitFL"));
            } else if (amountSansDebitFL != null) {
                totalDebitFL = recordAmountDebitFL.add(amountSansDebitFL);
            } else {
                totalDebitFL = recordAmountDebitFL;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditFL") != null
                    && amountSansCreditFL != null) {
                totalCreditFL = recordAmountCreditFL.add(amountSansCreditFL)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccCreditFL"));
            } else if (amountSansCreditFL != null) {
                totalCreditFL = recordAmountCreditFL.add(amountSansCreditFL);
            } else {
                totalCreditFL = recordAmountCreditFL;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccDebitUSD") != null
                    && amountSansDebitUSD != null) {
                totalDebitUSD = recordAmountDebitUSD.add(amountSansDebitUSD)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccDebitUSD"));
            } else if (amountSansDebitUSD != null) {
                totalDebitUSD = recordAmountDebitUSD.add(amountSansDebitUSD);
            } else {
                totalDebitUSD = recordAmountDebitUSD;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditUSD") != null
                    && amountSansCreditUSD != null) {
                totalCreditUSD = recordAmountCreditUSD.add(amountSansCreditUSD)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccCreditUSD"));
            } else if (amountSansCreditUSD != null) {
                totalCreditUSD = recordAmountCreditUSD.add(amountSansCreditUSD);
            } else {
                totalCreditUSD = recordAmountCreditUSD;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccDebitEUR") != null
                    && amountSansDebitEUR != null) {
                totalDebitEUR = recordAmountDebitEUR.add(amountSansDebitEUR)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccDebitEUR"));
            } else if (amountSansDebitEUR != null) {
                totalDebitEUR = recordAmountDebitEUR.add(amountSansDebitEUR);
            } else {
                totalDebitEUR = recordAmountDebitEUR;
            }

            if (retrievedAccountNumberAmounts.get("FaultyAccCreditEUR") != null
                    && amountSansCreditEUR != null) {
                totalCreditEUR = recordAmountCreditEUR.add(amountSansCreditEUR)
                        .subtract(
                                retrievedAccountNumberAmounts
                                        .get("FaultyAccCreditEUR"));
            } else if (amountSansCreditEUR != null) {
                totalCreditEUR = recordAmountCreditEUR.add(amountSansCreditEUR);
            } else {
                totalCreditEUR = recordAmountCreditEUR;
            }

            recordAmountFL = totalDebitFL.subtract(totalCreditFL).abs();
            recordAmountUSD = totalDebitUSD.subtract(totalCreditUSD).abs();
            recordAmountEUR = totalDebitEUR.subtract(totalCreditEUR).abs();

            retrievedAmounts.put(Constants.CURRENCY_EURO, recordAmountEUR);
            retrievedAmounts.put(Constants.CURRENCY_FL, recordAmountUSD);
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
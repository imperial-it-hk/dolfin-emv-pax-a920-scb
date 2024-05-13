package com.pax.pay.ECR;

public interface PosNetCommInterface {
    int onDataRcv(byte[] transactionType, byte [] data, boolean isVatb,byte [] REF1, byte [] REF2,byte [] vatAmount,byte [] taxAllowance,byte [] mercUniqueValue
                  ,byte [] campaignType, byte[] PosNo_ReceiptNo, byte[] CashierName);
}

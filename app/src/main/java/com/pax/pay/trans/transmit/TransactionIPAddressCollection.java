package com.pax.pay.trans.transmit;

import java.util.List;

public class TransactionIPAddressCollection {
    private List<TransactionIPAddress> list;
    private int selectedIndex;

    public TransactionIPAddressCollection(List<TransactionIPAddress> list, int selectedIndex) {
        this.list = list;
        this.selectedIndex = selectedIndex;
    }

    public TransactionIPAddress get() throws Exception {
        TransactionIPAddress selected = null;

        try {
            selected = list.get(this.selectedIndex);
        }
        catch (Exception ex) {
            throw ex;
        }

        return selected;
    }
    public TransactionIPAddress get(int SpecificIndex) throws Exception {
        TransactionIPAddress selected = null;

        try {
            selected = list.get(SpecificIndex);
        }
        catch (Exception ex) {
            throw ex;
        }

        return selected;
    }

    public void next() {
        int currentSelectedIndex = this.selectedIndex;
        currentSelectedIndex++;

        if (currentSelectedIndex < list.size()) {
            this.selectedIndex = currentSelectedIndex;
        } else {
            this.selectedIndex = 0;
        }
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        int result = 0;

        for (int index = 0; index < list.size(); index++) {
            TransactionIPAddress transactionIPAddress = list.get(index);
            if (transactionIPAddress.getIndex() == selectedIndex) {
                result = index;
                break;
            }
        }

        this.selectedIndex = result;
    }

    public boolean isContainIndex(int index) {
        boolean result = false;

        for (TransactionIPAddress transactionIPAddress: list) {
            if (transactionIPAddress.getIndex() == index) {
                result = true;
                break;
            }
        }

        return result;
    }


    public int getItemCount() {
        return this.list.size();
    }
}

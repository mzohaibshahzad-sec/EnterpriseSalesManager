package com.ledger.salesmanager.model;

public class StoreInfo {
    private int id;
    private String storeName;
    private String storeLogoPath;
    private String storeAddress;
    private String storeContact;
    private String storeEmail;
    private String currencySymbol = "Rs";
    private String theme = "DARK";
    private boolean setupCompleted;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreLogoPath() { return storeLogoPath; }
    public void setStoreLogoPath(String storeLogoPath) { this.storeLogoPath = storeLogoPath; }
    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }
    public String getStoreContact() { return storeContact; }
    public void setStoreContact(String storeContact) { this.storeContact = storeContact; }
    public String getStoreEmail() { return storeEmail; }
    public void setStoreEmail(String storeEmail) { this.storeEmail = storeEmail; }
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isSetupCompleted() { return setupCompleted; }
    public void setSetupCompleted(boolean setupCompleted) { this.setupCompleted = setupCompleted; }
}

package com.iolab.sdtecsynttest;

import android.location.Address;

public class GeoSearchResult {

    private Address address;

    public GeoSearchResult(Address address){
        this.address = address;
    }

    public String getAddress(){
        String display_address = "";
        for(int i = 0; i < address.getMaxAddressLineIndex(); i++){
            display_address += address.getAddressLine(i) + ", ";
        }
        return display_address;
    }

    public String toString(){
        String display_address = "";

        if(address.getFeatureName() != null){
            display_address += address + ", ";
        }

        for(int i = 0; i < address.getMaxAddressLineIndex(); i++){
            display_address += address.getAddressLine(i);
        }
        return display_address;
    }
}
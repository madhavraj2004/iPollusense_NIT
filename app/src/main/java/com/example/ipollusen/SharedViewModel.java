package com.example.ipollusen;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> data = new MutableLiveData<>();

    public void setData(String data) {
        this.data.setValue(data);
    }

    public LiveData<String> getData() {
        return data;
    }
}

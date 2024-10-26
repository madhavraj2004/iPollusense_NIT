package com.example.ipollusen;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<SensorData> sensorData = new MutableLiveData<>();

    public void setSensorData(SensorData data) {
        sensorData.setValue(data);
    }

    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }
}
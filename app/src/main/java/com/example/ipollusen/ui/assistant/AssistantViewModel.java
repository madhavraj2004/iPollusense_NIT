package com.example.ipollusen.ui.assistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AssistantViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AssistantViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
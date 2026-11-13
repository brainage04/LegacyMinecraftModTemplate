package com.example.config.categories;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MainCategory {
    @Expose
    @ConfigOption(name = "Toggle (Boolean)", desc = "This is a boolean used to toggle things on and off.")
    @ConfigEditorBoolean
    public boolean testBoolean = false;
}

package com.example.config;

import com.example.ExampleMod;
import com.example.config.categories.MainCategory;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

public class ExampleModConfig extends Config {

    @Override
    public StructuredText getTitle() {
        return StructuredText.of(ExampleMod.MOD_NAME + " " + ExampleMod.getVersion() + " by §cbrainage04§r, config by §5Moulberry §rand §5nea89");
    }

    @Override
    public void saveNow() {
        ExampleMod.configManager.save();
    }

    @Expose
    @Category(name = "Main Category", desc = "This is the main category.")
    public MainCategory mainCategory = new MainCategory();
}

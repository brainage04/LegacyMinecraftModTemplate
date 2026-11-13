package com.example.config.categories;

import com.example.utils.ChatUtils;
import com.example.utils.SoundUtils;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class TypeExamples {
    @Expose
    @ConfigOption(name = "Toggle (Boolean)", desc = "This is a boolean used to toggle things on and off.")
    @ConfigEditorBoolean
    public boolean testBoolean = false;

    @Expose
    @ConfigOption(name = "Slider (Integer)", desc = "This is an integer used to choose between a range of integers.")
    @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
    public int testNumber = 5;

    @Expose
    @ConfigOption(name = "Dropdown (Integer)", desc = "This is an integer used to choose one of several specified options (short range of integers).")
    @ConfigEditorDropdown(values = {"Option 1", "Option 2", "Option 3"})
    public int testDropdown = 0;

    @Expose
    @ConfigOption(name = "Dropdown (Property)", desc = "This is an integer (in Property form) used to choose one of several specified options (short range of integers).")
    @ConfigEditorDropdown
    public Property<TestProperty> testDropdownProperty = Property.of(TestProperty.TEST1);

    public enum TestProperty {
        TEST1("Test 1"),
        TEST2("Test 2"),
        TEST3("Test 3");

        private final String label;

        TestProperty(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Expose
    @ConfigOption(name = "List (Integer List)", desc = "This is a list of integers used to choose a permutation of several specified options (short range of integers that can be ordered at will).")
    @ConfigEditorDraggableList(exampleText = {"Option 1", "Option 2", "Option 3"})
    public List<Integer> testList = Arrays.asList(0, 1, 2);

    @ConfigOption(name = "Button (Runnable)", desc = "This is a button that runs a function (() -> { function goes here }) when clicked.")
    @ConfigEditorButton(buttonText = "Click")
    public Runnable testButton = () -> ChatUtils.messageToChat("Test", SoundUtils.SoundType.NOTIFICATION);

    @Expose
    @ConfigOption(name = "Text (String)", desc = "This is a string.")
    @ConfigEditorText
    public String testString = "Test";

    @Expose
    @ConfigOption(name = "Keybind (Integer)", desc = "This is an integer used to detect when a keyboard key is pressed using the Keyboard module, native to Java (slightly different handling to the KeyBinding module, native to Minecraft).")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_V)
    public int testKeybind;
}

package org.freeplane.features.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

public class OSKeyBindingManager {

    private static final UIDefaults systemDefaults;
    private static final Set<LookAndFeel> patchedLAFs = Collections.newSetFromMap(new WeakHashMap<>());

    private static final List<String> inputMapKeys = Arrays.asList(
        "TextField.focusInputMap", "TextArea.focusInputMap", "PasswordField.focusInputMap",
        "EditorPane.focusInputMap", "FormattedTextField.focusInputMap",
        "Spinner.editorInputMap", "ComboBox.ancestorInputMap", "Tree.focusInputMap",
        "List.focusInputMap", "Table.ancestorInputMap", "TableHeader.ancestorInputMap",
        "CheckBox.focusInputMap", "RadioButton.focusInputMap", "Button.focusInputMap",
        "ToggleButton.focusInputMap", "RootPane.defaultButtonWindowKeyBindings"
    );

    private static final List<String> actionMapKeys = Arrays.asList(
        "TextField.actionMap", "TextArea.actionMap", "PasswordField.actionMap",
        "EditorPane.actionMap", "FormattedTextField.actionMap",
        "Spinner.actionMap", "ComboBox.actionMap", "Tree.actionMap",
        "List.actionMap", "Table.actionMap", "TableHeader.actionMap",
        "CheckBox.actionMap", "RadioButton.actionMap", "Button.actionMap",
        "ToggleButton.actionMap", "RootPane.actionMap"
    );

    private static final List<String> editableComponentInputMaps = Arrays.asList(
        "Tree.focusInputMap", "List.focusInputMap", "Table.ancestorInputMap"
    );

    static {
        UIDefaults captured;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            captured = UIManager.getLookAndFeelDefaults();
        } catch (Exception e) {
            e.printStackTrace();
            captured = new UIDefaults();
        }
        systemDefaults = captured;
    }

    public static void initialize() {/* trigger static block */};

    public static void applyToCurrentLookAndFeel() {
        if (!shouldApplyChanges()) {
            return;
        }

        LookAndFeel laf = UIManager.getLookAndFeel();
        UIDefaults targetDefaults = UIManager.getLookAndFeelDefaults();

        // Process input and action maps
        Object[][] preservedBindings = collectPreservedBindings(targetDefaults);
        applySystemDefaults(targetDefaults);
        restorePreservedBindings(targetDefaults, preservedBindings);
        ensureF2EditBinding(targetDefaults);

        patchedLAFs.add(laf);
    }

    private static boolean shouldApplyChanges() {
        if (systemDefaults == null) {
            return false;
        }

        LookAndFeel laf = UIManager.getLookAndFeel();

        if (laf.getClass().getName().equals(UIManager.getSystemLookAndFeelClassName())) {
            return false;
        }

        if (patchedLAFs.contains(laf)) {
            return false;
        }

        return true;
    }

    private static Object[][] collectPreservedBindings(UIDefaults targetDefaults) {
        List<KeyStroke> preservedKeys = preservedKeys();
        Object[][] preservedBindings = new Object[inputMapKeys.size() * preservedKeys.size()][3];
        int bindingCount = 0;

        for (String mapKey : inputMapKeys) {
            Object mapObj = targetDefaults.get(mapKey);
            if (mapObj instanceof InputMap) {
                InputMap targetMap = (InputMap) mapObj;
                for (KeyStroke keyStroke : preservedKeys) {
                    Object action = targetMap.get(keyStroke);
                    if (action != null) {
                        preservedBindings[bindingCount][0] = mapKey;
                        preservedBindings[bindingCount][1] = keyStroke;
                        preservedBindings[bindingCount][2] = action;
                        bindingCount++;
                    }
                }
            }
        }

        // Resize array to actual used size
        if (bindingCount < preservedBindings.length) {
            Object[][] trimmedBindings = new Object[bindingCount][3];
            System.arraycopy(preservedBindings, 0, trimmedBindings, 0, bindingCount);
            return trimmedBindings;
        }

        return preservedBindings;
    }

    private static void applySystemDefaults(UIDefaults targetDefaults) {
        // Apply system input maps
        for (String key : inputMapKeys) {
            Object systemValue = systemDefaults.get(key);
            if (systemValue instanceof InputMap && systemValue instanceof UIResource) {
                Object targetValue = targetDefaults.get(key);
                if (targetValue instanceof UIResource) {
                    targetDefaults.put(key, systemValue);
                }
            }
        }

        // Apply system action maps
        for (String key : actionMapKeys) {
            Object systemValue = systemDefaults.get(key);
            if (systemValue instanceof ActionMap && systemValue instanceof UIResource) {
                Object targetValue = targetDefaults.get(key);
                if (targetValue instanceof UIResource) {
                    targetDefaults.put(key, systemValue);
                }
            }
        }
    }

    private static void restorePreservedBindings(UIDefaults targetDefaults, Object[][] preservedBindings) {
        for (Object[] binding : preservedBindings) {
            String mapKey = (String) binding[0];

            Object mapObj = targetDefaults.get(mapKey);
            if (mapObj instanceof InputMap) {
                InputMap map = (InputMap) mapObj;
                KeyStroke keyStroke = (KeyStroke) binding[1];
                Object action = binding[2];
                map.put(keyStroke, action);
            }
        }
    }

    private static List<KeyStroke> preservedKeys() {
        List<KeyStroke> preservedKeys = new ArrayList<>();
        String[] arrowKeys = {"UP", "DOWN", "LEFT", "RIGHT"};
        String[] modifiers = {"", "shift", "ctrl", "meta"};

        for (String arrow : arrowKeys) {
            for (String modifier : modifiers) {
                String keyStrokeStr = modifier.isEmpty() ? arrow : modifier + " " + arrow;
                preservedKeys.add(KeyStroke.getKeyStroke(keyStrokeStr));
            }
        }
        return preservedKeys;
    }

    private static void ensureF2EditBinding(UIDefaults defaults) {
        KeyStroke f2 = KeyStroke.getKeyStroke("F2");

        for (String key : editableComponentInputMaps) {
            Object mapObj = defaults.get(key);
            if (mapObj instanceof InputMap) {
                InputMap im = (InputMap) mapObj;
                if (key.equals("Tree.focusInputMap")
                    || key.equals("Table.ancestorInputMap")
                    || key.equals("List.focusInputMap")) {
                    im.put(f2, "startEditing");
                }
            }
        }
    }

    private OSKeyBindingManager() {
        // Utility class
    }
}


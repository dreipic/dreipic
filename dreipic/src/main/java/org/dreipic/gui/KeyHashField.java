package org.dreipic.gui;

import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.xml.bind.DatatypeConverter;

public final class KeyHashField {
    private final JTextField field;

    private byte[] value;

    public KeyHashField() {
        field = new JTextField(8);
        field.setEditable(false);

        SwingUtils.onMouseClicked(field, e -> {
            if (e.getButton() == MouseEvent.BUTTON3) {
                String str = "";
                if (value != null && field.getText().isEmpty()) {
                    str = DatatypeConverter.printHexBinary(value);
                }
                field.setText(str);
            }
        });
    }

    public void setValue(byte[] value) {
        this.value = value == null ? null : Arrays.copyOf(value, Math.min(value.length, 4));
        field.setText("");
    }

    public JComponent component() {
        return field;
    }
}

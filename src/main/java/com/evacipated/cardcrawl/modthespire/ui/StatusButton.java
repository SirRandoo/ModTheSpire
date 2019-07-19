package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;

public class StatusButton extends JButton {
    public StatusButton(Icon icon) {
        this(icon, null);
    }

    public StatusButton(Icon icon, String tooltip) {
        super(null, icon);

        setMaximumSize(new Dimension(20, 20));
        setHideActionText(true);
        setContentAreaFilled(false);
        setSize(getMaximumSize());
        setToolTipText(tooltip);
    }
}

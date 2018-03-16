package com.evacipated.cardcrawl.modthespire.patches.modsscreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.PatchNotesScreen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class ModsScreen
{
    @SpireEnum
    public static MainMenuScreen.CurScreen MODS_LIST;

    private static final float START_Y = Settings.HEIGHT - 200.0F * Settings.scale;
    private float scrollY = START_Y;
    private float targetY = scrollY;
    private float scrollLowerBound;
    private float scrollUpperBound;
    private MenuCancelButton button = new MenuCancelButton();
    private boolean grabbedScreen = false;
    private float grabStartY = 0;

    private ArrayList<Hitbox> hitboxes = new ArrayList<>();
    private int selectedMod = -1;

    static Map<URL, Object> baseModBadges;
    private static Field ModBadge_x;
    private static Field ModBadge_y;
    private static Field ModBadge_modPanel;
    private static Method ModBadge_render;
    private static Method ModBadge_onClick;
    private static Field ModPanel_isUp;
    private static Method ModPanel_update;
    private static Field BaseMod_modSettingsUp;

    static
    {
        try {
            Class<?> ModBadge = ModsScreen.class.getClassLoader().loadClass("basemod.ModBadge");
            ModBadge_x = ModBadge.getDeclaredField("x");
            ModBadge_x.setAccessible(true);
            ModBadge_y = ModBadge.getDeclaredField("y");
            ModBadge_y.setAccessible(true);
            ModBadge_modPanel = ModBadge.getDeclaredField("modPanel");
            ModBadge_modPanel.setAccessible(true);
            ModBadge_render = ModBadge.getDeclaredMethod("receiveRender", SpriteBatch.class);
            ModBadge_render.setAccessible(true);
            ModBadge_onClick = ModBadge.getDeclaredMethod("onClick");
            ModBadge_onClick.setAccessible(true);

            Class<?> ModPanel = ModsScreen.class.getClassLoader().loadClass("basemod.ModPanel");
            ModPanel_isUp = ModPanel.getDeclaredField("isUp");
            ModPanel_isUp.setAccessible(true);
            ModPanel_update = ModPanel.getDeclaredMethod("update");
            ModPanel_update.setAccessible(true);

            BaseMod_modSettingsUp = ModsScreen.class.getClassLoader().loadClass("basemod.BaseMod").getDeclaredField("modSettingsUp");
        } catch (ClassNotFoundException e) {
            // NOP
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public ModsScreen()
    {
        for (int i=0; i<Loader.MODINFOS.length; ++i) {
            hitboxes.add(new Hitbox(430.0f * Settings.scale, 40.0f * Settings.scale));
        }
    }

    public void open()
    {
        button.show(PatchNotesScreen.TEXT[0]);
        targetY = Settings.HEIGHT - 150.0F * Settings.scale;
        scrollY = Settings.HEIGHT - 400.0F * Settings.scale;
        CardCrawlGame.mainMenuScreen.darken();
        CardCrawlGame.mainMenuScreen.screen = MODS_LIST;

        scrollUpperBound = targetY;
        scrollLowerBound = Settings.HEIGHT - 600.0F * Settings.scale;
    }

    public void update()
    {
        button.update();
        if (button.hb.clicked || InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            try {
                if (!(boolean)BaseMod_modSettingsUp.get(null)) {
                    CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                    button.hide();
                    CardCrawlGame.mainMenuScreen.lighten();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        updateScrolling();

        if (baseModBadges != null) {
            for (Map.Entry<URL, Object> entry : baseModBadges.entrySet()) {
                modPanel_update(entry.getValue());
            }
        }

        float tmpY = 0;
        for (int i=0; i<hitboxes.size(); ++i) {
            hitboxes.get(i).x = 90.0f * Settings.scale;
            hitboxes.get(i).y = tmpY + scrollY - 30.0f * Settings.scale;
            hitboxes.get(i).update();
            if (hitboxes.get(i).hovered && InputHelper.isMouseDown) {
                hitboxes.get(i).clickStarted = true;
            }
            tmpY -= 45.0f * Settings.scale;

            if (hitboxes.get(i).clicked) {
                hitboxes.get(i).clicked = false;
                selectedMod = i;
                if (baseModBadges != null) {
                    modBadge_onClick(baseModBadges.get(Loader.MODONLYURLS[i]));
                }
            }
        }

        InputHelper.justClickedLeft = false;
    }

    private void updateScrolling()
    {
        int y = InputHelper.mY;
        if (!grabbedScreen) {
            if (InputHelper.scrolledDown) {
                targetY += Settings.SCROLL_SPEED;
            } else if (InputHelper.scrolledUp) {
                targetY -= Settings.SCROLL_SPEED;
            }
            if (InputHelper.justClickedLeft) {
                grabbedScreen = true;
                grabStartY = y - targetY;
            }
        } else if (InputHelper.isMouseDown) {
            targetY = y - grabStartY;
        } else {
            grabbedScreen = false;
        }
        scrollY = MathHelper.scrollSnapLerpSpeed(scrollY, targetY);
        resetScrolling();
    }

    private void resetScrolling()
    {
        if (targetY < scrollLowerBound) {
            targetY = MathHelper.scrollSnapLerpSpeed(targetY, scrollLowerBound);
        } else if (targetY > scrollUpperBound) {
            targetY = MathHelper.scrollSnapLerpSpeed(targetY, scrollUpperBound);
        }
    }

    public void render(SpriteBatch sb)
    {
        FontHelper.renderFontCentered(sb, FontHelper.SCP_cardTitleFont_small, "Mod List",
            Settings.WIDTH / 2.0f,
            Settings.HEIGHT - 70.0f * Settings.scale,
            Settings.GOLD_COLOR);

        renderModList(sb);

        try {
            if (baseModBadges != null && (boolean)BaseMod_modSettingsUp.get(null)) {
                for (Map.Entry<URL, Object> entry : baseModBadges.entrySet()) {
                    try {
                            MainMenuScreen.CurScreen tmpScreen = CardCrawlGame.mainMenuScreen.screen;
                            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                            ModBadge_render.invoke(entry.getValue(), sb);
                            CardCrawlGame.mainMenuScreen.screen = tmpScreen;
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        for (Hitbox hitbox : hitboxes) {
            hitbox.render(sb);
        }

        button.render(sb);
    }

    private void renderModList(SpriteBatch sb)
    {
        OrthographicCamera camera = null;
        try {
            Field f = CardCrawlGame.class.getDeclaredField("camera");
            f.setAccessible(true);
            camera = (OrthographicCamera)f.get(Gdx.app.getApplicationListener());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        if (camera != null) {
            sb.flush();
            Rectangle scissors = new Rectangle();
            float y = Settings.HEIGHT - 110 * Settings.scale;
            Rectangle clipBounds = new Rectangle(50 * Settings.scale, y,
                500 * Settings.scale, button.hb.y - y + button.hb.height);
            ScissorStack.calculateScissors(camera, sb.getTransformMatrix(), clipBounds, scissors);
            ScissorStack.pushScissors(scissors);
        }

        sb.setColor(new Color(0, 0, 0, 0.8f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0F, 0.0F, Settings.WIDTH, Settings.HEIGHT);
        sb.setColor(Color.WHITE);

        float tmpY = 0;
        for (int i=0; i<Loader.MODONLYURLS.length; ++i) {
            if (hitboxes.get(i).hovered) {
                Color c = sb.getColor();
                sb.setColor(1, 1, 1, (hitboxes.get(i).clickStarted ? 0.8f : 0.4f));
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, hitboxes.get(i).x, hitboxes.get(i).y, hitboxes.get(i).width, hitboxes.get(i).height);
                sb.setColor(c);
            }

            final URL modURL = Loader.MODONLYURLS[i];
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.buttonLabelFont, Loader.MODINFOS[i].Name,
                95.0f * Settings.scale,
                tmpY + scrollY,
                Settings.CREAM_COLOR);
            if (i == selectedMod) {
                drawRect(sb,
                    hitboxes.get(i).x,
                    hitboxes.get(i).y,
                    hitboxes.get(i).width,
                    hitboxes.get(i).height,
                    2);
            }

            // Render BaseMod ModBadges
            if (baseModBadges != null) {
                for (Map.Entry<URL, Object> entry : baseModBadges.entrySet()) {
                    try {
                        if (entry.getKey().equals(modURL)) {
                            Object modBadge = entry.getValue();
                            ModBadge_x.set(modBadge, 55.0f * Settings.scale);
                            ModBadge_y.set(modBadge, tmpY + scrollY - 27.0f * Settings.scale);

                            boolean tmpModSettingsUp = (boolean)BaseMod_modSettingsUp.get(null);
                            BaseMod_modSettingsUp.set(null, false);
                            MainMenuScreen.CurScreen tmpScreen = CardCrawlGame.mainMenuScreen.screen;
                            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                            ModBadge_render.invoke(modBadge, sb);
                            CardCrawlGame.mainMenuScreen.screen = tmpScreen;
                            BaseMod_modSettingsUp.set(null, tmpModSettingsUp);
                            break;
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }

            tmpY -= 45.0f * Settings.scale;
        }

        if (camera != null) {
            sb.flush();
            ScissorStack.popScissors();
        }
    }

    private void drawRect(SpriteBatch sb, float x, float y, float width, float height, float thickness) {
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, width, thickness);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, thickness, height);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y+height-thickness, width, thickness);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x+width-thickness, y, thickness, height);
    }

    private void modPanel_update(Object badge)
    {
        try {
            Object modPanel = ModBadge_modPanel.get(badge);
            if (modPanel != null && (boolean)ModPanel_isUp.get(modPanel)) {
                ModPanel_update.invoke(modPanel);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void modBadge_onClick(Object badge)
    {
        try {
            ModBadge_onClick.invoke(badge);
            CardCrawlGame.mainMenuScreen.screen = MODS_LIST;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}

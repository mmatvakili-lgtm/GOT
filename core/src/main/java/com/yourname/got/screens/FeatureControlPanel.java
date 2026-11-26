package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.yourname.got.logic.system.FeatureSettings;
import com.yourname.got.logic.system.FeatureSettings.Group;
import com.yourname.got.logic.system.FeatureSettings.Spec;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The admin's Features Control Center: one page per optional feature (Bots, Seasonal Events, Research Tree) plus an
 * overview. Every setting has a plain explanation, a switch or a slider with its range, the default and when it
 * applies. Edits are staged: nothing is written until "Apply changes", which validates everything and saves it in one
 * atomic write. Presets give one-click starting points, and a live summary says in words what the current values do.
 */
public class FeatureControlPanel extends Table {
    private static final Color BG = new Color(0.09f, 0.10f, 0.13f, 0.97f);
    private static final Color CARD = new Color(0.15f, 0.17f, 0.22f, 1f);
    private static final Color SIDEBAR = new Color(0.11f, 0.12f, 0.16f, 1f);
    private static final Color ON = new Color(0.20f, 0.68f, 0.38f, 1f);
    private static final Color OFF = new Color(0.42f, 0.42f, 0.46f, 1f);
    private static final Color WARN = new Color(0.95f, 0.65f, 0.20f, 1f);
    private static final Color MUTED = new Color(0.66f, 0.69f, 0.76f, 1f);

    private enum Page { OVERVIEW, BOTS, EVENTS, RESEARCH }

    private final Skin skin;
    private final GameMediator mediator;
    private final Map<String, Integer> pending = new LinkedHashMap<>();
    private Page page = Page.OVERVIEW;
    private String message = "";
    private Color messageColor = MUTED;
    private final Runnable onBack;

    public FeatureControlPanel(Skin skin, GameMediator mediator, Runnable onBack) {
        this.skin = skin;
        this.mediator = mediator;
        this.onBack = onBack;
        setBackground(skin.newDrawable("white", BG));
        pad(14f);
        render();
    }

    // ------------------------------------------------------------------ values
    private int value(String key) {
        Integer p = pending.get(key);
        return p != null ? p : mediator.adminGetFeature(key);
    }

    private void stage(String key, int v) {
        Spec s = FeatureSettings.find(key);
        v = Math.max(s.min, Math.min(s.max, v));
        if (v == mediator.adminGetFeature(key)) pending.remove(key); else pending.put(key, v);
    }

    private boolean groupOn(Group g) {
        return switch (g) {
            case BOTS -> value("bots.count") > 0;
            case EVENTS -> value("events.enabled") == 1;
            case RESEARCH -> value("research.enabled") == 1;
        };
    }

    private static Color accent(Group g) {
        return switch (g) {
            case BOTS -> new Color(0.30f, 0.78f, 0.95f, 1f);
            case EVENTS -> new Color(0.98f, 0.76f, 0.25f, 1f);
            case RESEARCH -> new Color(0.72f, 0.52f, 0.98f, 1f);
        };
    }

    private static Group groupOf(Page p) {
        return switch (p) { case BOTS -> Group.BOTS; case EVENTS -> Group.EVENTS; case RESEARCH -> Group.RESEARCH; default -> null; };
    }

    // ------------------------------------------------------------------ building blocks
    private Table box(Color color) {
        Table t = new Table(skin);
        t.setBackground(skin.newDrawable("white", color));
        return t;
    }

    private Label text(String s, float scale, Color color) {
        Label l = new Label(s, skin);
        l.setFontScale(scale);
        l.setColor(color);
        l.setWrap(true);
        return l;
    }

    private Table badge(String s, Color color) {
        Table b = box(color);
        Label l = new Label(s, skin);
        l.setFontScale(0.6f);
        l.setColor(Color.WHITE);
        b.add(l).pad(3f, 9f, 3f, 9f);
        return b;
    }

    /** A big ON/OFF switch. Click toggles; onChange gets the new state. */
    private Table toggle(boolean on, final java.util.function.Consumer<Boolean> onChange, boolean enabled) {
        final Table t = box(on ? ON : OFF);
        Label l = new Label(on ? "ON" : "OFF", skin);
        l.setFontScale(0.8f);
        l.setColor(Color.WHITE);
        t.add(l).width(86f).height(34f).center();
        if (enabled) {
            t.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { onChange.accept(!on); }
            });
        } else {
            t.getColor().a = 0.45f;
        }
        return t;
    }

    private TextButton button(String label, final Runnable action) {
        TextButton b = new TextButton(label, skin);
        b.getLabel().setColor(Color.BLACK);
        b.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        return b;
    }

    private void go(Page p) { page = p; render(); }

    // ------------------------------------------------------------------ main layout
    private void render() {
        clearChildren();

        // header
        Table header = new Table();
        Table titles = new Table();
        Label title = new Label("Features Control Center", skin, "title");
        title.setFontScale(0.9f);
        titles.add(title).left().row();
        titles.add(text("Switch the optional features on or off and tune them. Admin only. Nothing is saved until you press Apply.",
            0.65f, MUTED)).width(720f).left();
        header.add(titles).left().expandX();
        if (!pending.isEmpty()) header.add(badge(pending.size() + " unsaved change(s)", WARN)).padRight(10f);
        header.add(button("Back to admin panel", onBack)).width(190f).height(34f);
        add(header).growX().padBottom(10f).row();

        // sidebar + page
        Table body = new Table();
        body.add(sidebar()).width(230f).top().padRight(12f);
        Table content = new Table();
        content.top().left();
        switch (page) {
            case OVERVIEW -> buildOverview(content);
            default -> buildGroupPage(content, groupOf(page));
        }
        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        body.add(scroll).width(820f).height(500f).top();
        add(body).row();

        // footer
        Table footer = new Table();
        Label msg = text(message, 0.7f, messageColor);
        footer.add(msg).width(520f).left().expandX();
        footer.add(button("Discard changes", () -> {
            pending.clear();
            message = "Unsaved changes were discarded.";
            messageColor = MUTED;
            render();
        })).width(170f).height(38f).padRight(8f);
        TextButton apply = button("Apply changes", this::apply);
        apply.setDisabled(pending.isEmpty());
        footer.add(apply).width(190f).height(38f);
        add(footer).growX().padTop(10f);
    }

    private Table sidebar() {
        Table side = box(SIDEBAR);
        side.pad(10f);
        side.top();
        side.add(navItem("Overview", null, Page.OVERVIEW)).growX().height(44f).padBottom(6f).row();
        side.add(navItem(Group.BOTS.title, Group.BOTS, Page.BOTS)).growX().height(44f).padBottom(6f).row();
        side.add(navItem(Group.EVENTS.title, Group.EVENTS, Page.EVENTS)).growX().height(44f).padBottom(6f).row();
        side.add(navItem(Group.RESEARCH.title, Group.RESEARCH, Page.RESEARCH)).growX().height(44f).padBottom(14f).row();
        side.add(text("Saved in balance.properties next to the game. Bots join when a new game starts; everything else "
            + "takes effect immediately.", 0.6f, MUTED)).width(200f).row();
        return side;
    }

    private Table navItem(String name, Group group, final Page target) {
        boolean active = page == target;
        Color base = group == null ? new Color(0.55f, 0.60f, 0.70f, 1f) : accent(group);
        Table item = box(active ? new Color(base.r * 0.55f, base.g * 0.55f, base.b * 0.55f, 1f) : CARD);
        Label l = new Label(name, skin);
        l.setFontScale(0.8f);
        l.setColor(Color.WHITE);
        item.add(l).left().expandX().padLeft(10f);
        if (group != null) {
            boolean on = groupOn(group);
            item.add(badge(on ? "ON" : "OFF", on ? ON : OFF)).padRight(8f);
        }
        item.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { go(target); }
        });
        return item;
    }

    // ------------------------------------------------------------------ overview
    private void buildOverview(Table content) {
        content.add(text("What is in here?", 0.9f, Color.WHITE)).left().row();
        content.add(text("The base game is unchanged until you switch something on. Each feature below is off by default. "
            + "Open its page to read how it works and to tune it, or use the quick switch here.", 0.7f, MUTED)).width(790f).left().padBottom(10f).row();
        for (final Group g : Group.values()) {
            Table card = box(CARD);
            card.pad(12f);
            Table info = new Table();
            Label name = new Label(g.title, skin);
            name.setColor(accent(g));
            info.add(name).left().row();
            info.add(text(g.summary, 0.7f, Color.WHITE)).width(480f).left().row();
            info.add(text(summary(g), 0.65f, MUTED)).width(480f).left().padTop(4f).row();
            card.add(info).left().expandX();
            Table right = new Table();
            right.add(toggle(groupOn(g), on -> setMaster(g, on), true)).padBottom(6f).row();
            right.add(button("Open settings", () -> go(pageOf(g)))).width(150f).height(32f);
            card.add(right).right();
            content.add(card).width(790f).padBottom(10f).row();
        }
    }

    private static Page pageOf(Group g) {
        return switch (g) { case BOTS -> Page.BOTS; case EVENTS -> Page.EVENTS; case RESEARCH -> Page.RESEARCH; };
    }

    private void setMaster(Group g, boolean on) {
        switch (g) {
            case BOTS -> stage("bots.count", on ? Math.max(2, FeatureSettings.find("bots.count").def) : 0);
            case EVENTS -> stage("events.enabled", on ? 1 : 0);
            case RESEARCH -> stage("research.enabled", on ? 1 : 0);
        }
        render();
    }

    // ------------------------------------------------------------------ feature pages
    private void buildGroupPage(Table content, final Group g) {
        Color accent = accent(g);

        Table head = box(CARD);
        head.pad(14f);
        Table titleBox = new Table();
        Label name = new Label(g.title, skin, "title");
        name.setFontScale(0.8f);
        name.setColor(accent);
        titleBox.add(name).left().row();
        titleBox.add(text(g.summary, 0.7f, Color.WHITE)).width(520f).left();
        head.add(titleBox).left().expandX();
        head.add(toggle(groupOn(g), on -> setMaster(g, on), true));
        content.add(head).width(790f).padBottom(8f).row();

        content.add(howItWorks(g, accent)).width(790f).padBottom(8f).row();
        content.add(presets(g)).width(790f).padBottom(8f).row();

        Table live = box(new Color(accent.r * 0.25f, accent.g * 0.25f, accent.b * 0.25f, 1f));
        live.pad(10f);
        live.add(text("With these settings: " + summary(g), 0.75f, Color.WHITE)).width(760f).left();
        content.add(live).width(790f).padBottom(8f).row();

        boolean masterOn = groupOn(g);
        for (Spec s : FeatureSettings.SPEC) {
            if (s.group != g) continue;
            boolean isMaster = s.key.equals("bots.count") || s.key.equals("events.enabled") || s.key.equals("research.enabled");
            if (s.key.equals("events.enabled") || s.key.equals("research.enabled")) continue; // the big switch above is that setting
            content.add(settingRow(s, masterOn || isMaster, accent)).width(790f).padBottom(6f).row();
        }
        content.add(button("Reset this page to defaults", () -> {
            for (Spec s : FeatureSettings.SPEC) if (s.group == g) stage(s.key, s.def);
            render();
        })).width(260f).height(34f).left().padTop(6f).row();
    }

    private Table howItWorks(Group g, Color accent) {
        Table t = box(CARD);
        t.pad(12f);
        t.add(text("How it works", 0.8f, accent)).left().row();
        String body = switch (g) {
            case BOTS -> "Bots are extra kingdoms controlled by the game, so you can play alone or fill empty slots. They exist only "
                + "inside the game file (they are not accounts and never appear in the user list or the leaderboard) and carry the "
                + "name [BOT]. They defend with their soldiers; they never attack. Optional: they can grow, build an economy and "
                + "train soldiers. A game with only bots left ends when no real player is alive.";
            case EVENTS -> "Once per game day the game rolls a die (the same for everyone). On a hit, the season decides the event: "
                + "Bountiful Harvest (+food) in summer and fall, Famine (-food) in winter, Trade Windfall (+coin) in spring. "
                + "Every kingdom, players and bots alike, is affected by the same percent, and a notice is sent to the players. "
                + "A day is never rolled twice, even after saving and loading.";
            case RESEARCH -> "Players spend resources and time to learn technologies from the Research Tree (Extra menu): 15 technologies "
                + "in Economy, Military and Defense, up to 3 levels each, with prerequisites and castle-level requirements. "
                + "Bonuses are permanent percentages (production, storage, training and march speed, attack, toughness, loot, towers). "
                + "Switching research off stops all bonuses; learned technologies are kept for when it is switched on again.";
        };
        t.add(text(body, 0.7f, Color.WHITE)).width(760f).left();
        return t;
    }

    private Table presets(final Group g) {
        Table t = box(CARD);
        t.pad(10f);
        t.add(text("Quick presets", 0.8f, Color.WHITE)).left().padRight(14f);
        for (final String name : presetNames(g)) {
            t.add(button(name, () -> {
                for (Map.Entry<String, Integer> e : presetValues(g, name).entrySet()) stage(e.getKey(), e.getValue());
                message = name + " preset loaded - press Apply changes to save it.";
                messageColor = WARN;
                render();
            })).width(110f).height(32f).padRight(8f);
        }
        return t;
    }

    private static String[] presetNames(Group g) {
        return switch (g) {
            case BOTS -> new String[]{"Off", "Easy", "Normal", "Hard"};
            case EVENTS -> new String[]{"Off", "Gentle", "Normal", "Wild"};
            case RESEARCH -> new String[]{"Off", "Fast", "Normal", "Slow"};
        };
    }

    private static Map<String, Integer> presetValues(Group g, String name) {
        Map<String, Integer> m = new LinkedHashMap<>();
        switch (g) {
            case BOTS -> {
                switch (name) {
                    case "Off" -> m.put("bots.count", 0);
                    case "Easy" -> { m.put("bots.count", 2); m.put("bots.garrison", 20); m.put("bots.growth.perDay", 0);
                        m.put("bots.build.enabled", 0); m.put("bots.train.perDay", 0); }
                    case "Normal" -> { m.put("bots.count", 2); m.put("bots.garrison", 30); m.put("bots.growth.perDay", 2);
                        m.put("bots.garrison.max", 120); m.put("bots.build.enabled", 1); m.put("bots.train.perDay", 0); }
                    default -> { m.put("bots.count", 3); m.put("bots.garrison", 60); m.put("bots.growth.perDay", 5);
                        m.put("bots.garrison.max", 300); m.put("bots.build.enabled", 1); m.put("bots.train.perDay", 5); }
                }
            }
            case EVENTS -> {
                switch (name) {
                    case "Off" -> m.put("events.enabled", 0);
                    case "Gentle" -> { m.put("events.enabled", 1); m.put("events.chance.percent", 8); m.put("events.effect.percent", 5); }
                    case "Normal" -> { m.put("events.enabled", 1); m.put("events.chance.percent", 15); m.put("events.effect.percent", 10); }
                    default -> { m.put("events.enabled", 1); m.put("events.chance.percent", 35); m.put("events.effect.percent", 25); }
                }
            }
            case RESEARCH -> {
                switch (name) {
                    case "Off" -> m.put("research.enabled", 0);
                    case "Fast" -> { m.put("research.enabled", 1); m.put("research.cost.percent", 50); m.put("research.time.percent", 50); }
                    case "Normal" -> { m.put("research.enabled", 1); m.put("research.cost.percent", 100); m.put("research.time.percent", 100); }
                    default -> { m.put("research.enabled", 1); m.put("research.cost.percent", 150); m.put("research.time.percent", 200); }
                }
            }
        }
        return m;
    }

    private Table settingRow(final Spec s, boolean enabled, Color accent) {
        Table row = box(CARD);
        row.pad(10f);
        Table info = new Table();
        Label name = new Label(s.label, skin);
        name.setFontScale(0.85f);
        name.setColor(enabled ? Color.WHITE : MUTED);
        info.add(name).left().row();
        info.add(text(s.description, 0.62f, MUTED)).width(430f).left().row();
        info.add(text(s.applies + "   -   default " + s.def + s.unit + "   -   range " + s.min + " to " + s.max,
            0.55f, new Color(0.5f, 0.55f, 0.65f, 1f))).width(430f).left().padTop(2f).row();
        row.add(info).left().expandX();

        Table control = new Table();
        final String key = s.key;
        if (s.isSwitch()) {
            control.add(toggle(value(key) == 1, on -> { stage(key, on ? 1 : 0); render(); }, enabled));
        } else {
            final Slider slider = new Slider(s.min, s.max, 1, false, skin);
            slider.setValue(value(key));
            slider.setDisabled(!enabled);
            final Label shown = new Label(value(key) + s.unit, skin);
            shown.setFontScale(0.8f);
            shown.setColor(accent);
            slider.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    int v = Math.round(slider.getValue());
                    shown.setText(v + s.unit);
                    stage(key, v); // cheap: the header counter refreshes on the next full render
                }
            });
            // a slider can also be released with the pending counter refreshed
            slider.addListener(new ClickListener() {
                @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    super.touchUp(event, x, y, pointer, button);
                    render();
                }
            });
            control.add(slider).width(240f).row();
            control.add(shown).right().row();
        }
        control.add(button("Default", () -> { stage(key, s.def); render(); })).width(90f).height(26f).right().padTop(4f);
        row.add(control).right().padLeft(10f);
        if (!enabled) row.getColor().a = 0.6f;
        return row;
    }

    // ------------------------------------------------------------------ plain-words summary
    private String summary(Group g) {
        switch (g) {
            case BOTS -> {
                int n = value("bots.count");
                if (n == 0) return "no bots join new games.";
                StringBuilder sb = new StringBuilder(n + " bot(s) join each new game with " + value("bots.garrison") + " soldiers");
                if (value("bots.growth.perDay") > 0) sb.append(", gain ").append(value("bots.growth.perDay")).append(" per day up to ").append(value("bots.garrison.max"));
                if (value("bots.build.enabled") == 1) sb.append(", build an economy");
                if (value("bots.train.perDay") > 0) sb.append(", train ").append(value("bots.train.perDay")).append(" soldiers per day");
                return sb.append(". They only defend.").toString();
            }
            case EVENTS -> {
                if (value("events.enabled") == 0) return "no seasonal events happen.";
                return "about " + value("events.chance.percent") + "% of game days bring an event worth " + value("events.effect.percent")
                    + "% of storage, the same for every kingdom.";
            }
            default -> {
                if (value("research.enabled") == 0) return "research is closed; no technology bonuses apply.";
                int cost = value("research.cost.percent"), time = value("research.time.percent");
                return "research is open at " + cost + "% of the listed cost and " + time + "% of the listed time"
                    + (cost < 100 || time < 100 ? " (faster/cheaper than normal)." : cost > 100 || time > 100 ? " (slower/costlier than normal)." : " (normal).");
            }
        }
    }

    // ------------------------------------------------------------------ apply
    private void apply() {
        if (pending.isEmpty()) return;
        Result r = mediator.adminSetFeatures(new LinkedHashMap<>(pending));
        if (r.isSuccess()) {
            pending.clear();
            message = "Saved. " + r.getMessage() + " Bots join the next game that is started; the rest is already active.";
            messageColor = ON;
        } else {
            message = r.getMessage();
            messageColor = new Color(1f, 0.45f, 0.4f, 1f);
        }
        render();
    }
}

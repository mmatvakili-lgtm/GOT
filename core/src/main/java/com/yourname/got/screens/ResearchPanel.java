package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.research.Branch;
import com.yourname.got.logic.research.Effect;
import com.yourname.got.logic.research.Research;
import com.yourname.got.logic.research.ResearchState;
import com.yourname.got.logic.research.Tech;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The research screen: the tree (one column per branch, one row per tier, lines between a technology and its
 * prerequisites), a detail panel with the cost / time / requirements of the selected technology, the running
 * research with a live progress bar, the queue with cancel buttons, the resources you have and the total bonuses.
 * It refreshes itself about once a second, so progress and affordability stay current while it is open.
 */
public class ResearchPanel extends Table {
    private static final float NODE_W = 142f, NODE_H = 58f, SLOT_W = 150f, BRANCH_W = 316f, TIER_H = 88f;
    private static final Color LOCKED = new Color(0.28f, 0.28f, 0.30f, 1f);
    private static final Color AVAILABLE = new Color(0.16f, 0.45f, 0.22f, 1f);
    private static final Color POOR = new Color(0.62f, 0.42f, 0.12f, 1f);
    private static final Color QUEUED = new Color(0.17f, 0.34f, 0.62f, 1f);
    private static final Color MAXED = new Color(0.70f, 0.58f, 0.15f, 1f);

    private final Skin skin;
    private final GameMediator mediator;
    private final Kingdom kingdom;

    private final Map<Tech, Table> nodes = new EnumMap<>(Tech.class);
    private final Map<Tech, Label> pips = new EnumMap<>(Tech.class);
    private final Map<Tech, Image> borders = new EnumMap<>(Tech.class);
    private final Group tree = new Group();
    private final Table detail = new Table();
    private final Table queueBox = new Table();
    private final Table summaryBox = new Table();
    private final Label resources;
    private final Label status;
    private Tech selected = Tech.TOOLS;
    private float sinceRefresh = 0f;

    public ResearchPanel(Skin skin, GameMediator mediator) {
        this.skin = skin;
        this.mediator = mediator;
        this.kingdom = mediator.getCurrentUser() == null ? null : mediator.getCurrentUser().getKingdom();
        pad(10f);

        resources = new Label("", skin);
        resources.setFontScale(0.8f);
        status = new Label("", skin);
        status.setFontScale(0.75f);
        status.setWrap(true);

        buildTree();

        ScrollPane treeScroll = new ScrollPane(tree, skin);
        treeScroll.setFadeScrollBars(false);

        Table left = new Table();
        left.add(resources).left().padBottom(6f).row();
        left.add(branchHeaders()).left().row();
        left.add(treeScroll).width(3 * BRANCH_W + 10f).height(4 * TIER_H + 30f).row();
        left.add(queueBox).left().growX().padTop(8f).row();

        Table right = new Table();
        right.top();
        right.add(detail).width(330f).top().left().row();
        right.add(summaryBox).width(330f).top().left().padTop(10f).row();

        add(left).top().padRight(12f);
        add(right).top();
        row();
        add(status).colspan(2).left().growX().padTop(6f);

        if (!Research.enabled()) {
            status.setText("Research is switched off in this game (the admin can enable it). You can look at the tree, "
                + "but nothing can be researched and no bonus applies.");
            status.setColor(Color.SALMON);
        }
        refresh();
    }

    // ------------------------------------------------------------------ layout
    private Table branchHeaders() {
        Table t = new Table();
        for (Branch b : Branch.values()) {
            Label l = new Label(b.label + " - " + b.description, skin);
            l.setFontScale(0.75f);
            l.setColor(branchColor(b));
            t.add(l).width(BRANCH_W).left();
        }
        return t;
    }

    private static Color branchColor(Branch b) {
        return switch (b) {
            case ECONOMY -> new Color(0.95f, 0.85f, 0.35f, 1f);
            case MILITARY -> new Color(1f, 0.55f, 0.45f, 1f);
            case DEFENSE -> new Color(0.5f, 0.8f, 1f, 1f);
        };
    }

    private float nodeX(Tech t) {
        int slot = 0;
        for (Tech o : Tech.values()) {
            if (o == t) break;
            if (o.branch == t.branch && o.tier == t.tier) slot++;
        }
        return t.branch.ordinal() * BRANCH_W + 6f + slot * SLOT_W;
    }

    private float nodeY(Tech t, float height) {
        return height - (t.tier * TIER_H) + (TIER_H - NODE_H) / 2f - 8f;
    }

    private void buildTree() {
        float width = 3 * BRANCH_W, height = 4 * TIER_H + 20f;
        tree.setSize(width, height);

        // lines first, so the nodes are drawn on top of them
        for (Tech t : Tech.values()) {
            for (Tech.Requirement r : t.requirements()) {
                line(nodeX(r.tech) + NODE_W / 2f, nodeY(r.tech, height), nodeX(t) + NODE_W / 2f, nodeY(t, height) + NODE_H, r.tech);
            }
        }
        for (final Tech t : Tech.values()) {
            final Table node = new Table(skin);
            node.setSize(NODE_W, NODE_H);
            node.setPosition(nodeX(t), nodeY(t, height));
            Label title = new Label(t.title, skin);
            title.setFontScale(0.65f);
            title.setWrap(true);
            Label pip = new Label("", skin);
            pip.setFontScale(0.7f);
            pips.put(t, pip);
            node.add(title).width(NODE_W - 8f).row();
            node.add(pip);
            node.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selected = t;
                    refresh();
                }
            });
            Image border = new Image(skin.newDrawable("white", Color.WHITE));
            border.setBounds(nodeX(t) - 2f, nodeY(t, height) - 2f, NODE_W + 4f, NODE_H + 4f);
            border.setVisible(false);
            borders.put(t, border);
            tree.addActor(border);
            tree.addActor(node);
            nodes.put(t, node);
        }
    }

    /** A thin line from (x1, y1) to (x2, y2) drawn as a rotated bar. */
    private void line(float x1, float y1, float x2, float y2, Tech from) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        Image bar = new Image(skin.newDrawable("white", new Color(0.55f, 0.55f, 0.6f, 0.9f)));
        bar.setSize(len, 2f);
        bar.setOrigin(0f, 1f);
        bar.setPosition(x1, y1);
        bar.setRotation((float) Math.toDegrees(Math.atan2(dy, dx)));
        tree.addActor(bar);
    }

    // ------------------------------------------------------------------ refresh
    @Override
    public void act(float delta) {
        super.act(delta);
        sinceRefresh += delta;
        if (sinceRefresh >= 1f) {
            sinceRefresh = 0f;
            refresh();
        }
    }

    private int have(String type) {
        return kingdom == null ? 0 : kingdom.getTotalResources(type);
    }

    private void refresh() {
        if (kingdom == null) return;
        resources.setText("Wood " + have("Wood") + "   Stone " + have("Stone") + "   Coin " + have("COIN") + "   Food " + have("Food"));
        ResearchState state = kingdom.getResearch();

        for (Tech t : Tech.values()) {
            int level = Research.level(kingdom, t);
            int queued = Research.queuedLevels(kingdom, t);
            StringBuilder p = new StringBuilder();
            for (int i = 1; i <= t.maxLevel; i++) p.append(i <= level ? "[#]" : (i <= level + queued ? "[~]" : "[ ]"));
            pips.get(t).setText(p.toString());

            Color c;
            if (level >= t.maxLevel) c = MAXED;
            else if (queued > 0 && Research.nextLevel(kingdom, t) > t.maxLevel) c = QUEUED;
            else {
                Result r = Research.check(kingdom, t);
                if (r.isSuccess()) c = AVAILABLE;
                else if (r.getMessage().startsWith("Not enough") || r.getMessage().startsWith("The research queue")) c = POOR;
                else c = LOCKED;
                if (queued > 0 && !r.isSuccess() && c == LOCKED) c = QUEUED;
            }
            nodes.get(t).setBackground(skin.newDrawable("white", c));
            Image border = borders.get(t);
            border.setVisible(t == selected);
        }
        buildDetail();
        buildQueue(state);
        buildSummary();
    }

    private void buildDetail() {
        detail.clearChildren();
        final Tech t = selected;
        int level = Research.level(kingdom, t);
        int next = Research.nextLevel(kingdom, t);

        Label name = new Label(t.title, skin);
        name.setColor(branchColor(t.branch));
        detail.add(name).left().row();
        Label branch = new Label(t.branch.label + "  -  Tier " + t.tier + "  -  needs castle level " + t.castleLevel, skin);
        branch.setFontScale(0.65f);
        branch.setColor(Color.LIGHT_GRAY);
        detail.add(branch).left().row();
        Label desc = new Label(t.description, skin);
        desc.setFontScale(0.7f);
        desc.setWrap(true);
        detail.add(desc).width(320f).left().padTop(4f).row();
        Label eff = new Label(t.effectText() + "\nLevel " + level + "/" + t.maxLevel
            + "  (now +" + level * t.perLevelPercent + "%)", skin);
        eff.setFontScale(0.7f);
        eff.setColor(Color.GOLD);
        detail.add(eff).left().padTop(4f).row();

        if (!t.requirements().isEmpty()) {
            Label req = new Label("Requires:", skin);
            req.setFontScale(0.7f);
            detail.add(req).left().padTop(6f).row();
            for (Tech.Requirement r : t.requirements()) {
                boolean ok = Research.level(kingdom, r.tech) >= r.level;
                Label l = new Label((ok ? "[x] " : "[ ] ") + r.tech.title + " level " + r.level, skin);
                l.setFontScale(0.65f);
                l.setColor(ok ? Color.GREEN : Color.SALMON);
                detail.add(l).left().row();
            }
        }

        if (next <= t.maxLevel) {
            int[] cost = Research.cost(t, next);
            String[] names = {"Wood", "Stone", "Coin", "Food"};
            String[] keys = {"Wood", "Stone", "COIN", "Food"};
            Label head = new Label("Level " + next + " costs:", skin);
            head.setFontScale(0.7f);
            detail.add(head).left().padTop(6f).row();
            for (int i = 0; i < 4; i++) {
                if (cost[i] <= 0) continue;
                boolean ok = have(keys[i]) >= cost[i];
                Label l = new Label(names[i] + " " + cost[i] + "  (you have " + have(keys[i]) + ")", skin);
                l.setFontScale(0.65f);
                l.setColor(ok ? Color.GREEN : Color.SALMON);
                detail.add(l).left().row();
            }
            int speed = 1;
            if (kingdom.getOwner() != null && kingdom.getOwner().getGame() != null) speed = kingdom.getOwner().getGame().getSpeed();
            Label time = new Label("Time: " + Research.durationText(Research.durationMillis(kingdom, t, next, speed))
                + (Research.bonusPercent(kingdom, Effect.RESEARCH_SPEED) > 0
                    ? "  (research speed +" + Research.bonusPercent(kingdom, Effect.RESEARCH_SPEED) + "%)" : ""), skin);
            time.setFontScale(0.65f);
            detail.add(time).left().row();

            final Result check = Research.check(kingdom, t);
            TextButton go = new TextButton("Research level " + next, skin);
            go.setDisabled(!check.isSuccess());
            go.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (!check.isSuccess()) { say(check.getMessage(), false); return; }
                    Result r = mediator.researchStart(t);
                    say(r.getMessage(), r.isSuccess());
                    refresh();
                }
            });
            detail.add(go).width(250f).height(40f).left().padTop(8f).row();
            if (!check.isSuccess()) {
                Label why = new Label(check.getMessage(), skin);
                why.setFontScale(0.65f);
                why.setWrap(true);
                why.setColor(Color.SALMON);
                detail.add(why).width(320f).left().row();
            }
        } else {
            Label done = new Label("Fully researched.", skin);
            done.setColor(Color.GOLD);
            detail.add(done).left().padTop(8f).row();
        }
    }

    private void buildQueue(ResearchState state) {
        queueBox.clearChildren();
        Label head = new Label("Research queue (" + state.queue().size() + "/" + Research.MAX_QUEUE + ")", skin);
        head.setFontScale(0.75f);
        queueBox.add(head).left().colspan(3).row();
        List<ResearchState.Task> queue = state.queue();
        if (queue.isEmpty()) {
            Label none = new Label("Nothing is being researched.", skin);
            none.setFontScale(0.65f);
            none.setColor(Color.GRAY);
            queueBox.add(none).left().colspan(3).row();
            return;
        }
        ProgressBar.ProgressBarStyle barStyle = skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class);
        for (int i = 0; i < queue.size(); i++) {
            final int index = i;
            ResearchState.Task task = queue.get(i);
            Tech t = Tech.valueOf(task.techId);
            Label l = new Label((i == 0 ? "NOW  " : "#" + (i + 1) + "  ") + t.title + " " + task.level, skin);
            l.setFontScale(0.7f);
            l.setColor(i == 0 ? Color.GOLD : Color.WHITE);
            queueBox.add(l).left().width(210f);

            ProgressBar bar = new ProgressBar(0, Math.max(1, task.requiredMillis), 1, false, barStyle);
            long remaining = Research.remainingMillis(task);
            bar.setValue(task.started() ? task.requiredMillis - remaining : 0);
            queueBox.add(bar).width(220f).height(12f).padLeft(6f);

            Label eta = new Label(task.started() ? Research.durationText(remaining) : "waiting", skin);
            eta.setFontScale(0.65f);
            queueBox.add(eta).width(70f).padLeft(6f);

            TextButton cancel = new TextButton("Cancel", skin);
            cancel.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    Result r = mediator.researchCancel(index);
                    say(r.getMessage(), r.isSuccess());
                    refresh();
                }
            });
            queueBox.add(cancel).width(80f).height(26f).padLeft(6f).row();
        }
    }

    private void buildSummary() {
        summaryBox.clearChildren();
        Label head = new Label("Your bonuses", skin);
        head.setFontScale(0.75f);
        head.setColor(Color.GOLD);
        summaryBox.add(head).left().row();
        boolean any = false;
        for (Effect e : Effect.values()) {
            int pct = Research.bonusPercent(kingdom, e);
            if (pct <= 0) continue;
            any = true;
            Label l = new Label("+" + pct + "%  " + e.label, skin);
            l.setFontScale(0.65f);
            l.setColor(Color.GREEN);
            summaryBox.add(l).left().row();
        }
        if (!any) {
            Label none = new Label(Research.enabled() ? "No technology learned yet." : "Research is off: no bonuses apply.", skin);
            none.setFontScale(0.65f);
            none.setColor(Color.GRAY);
            summaryBox.add(none).left().row();
        }
        Label total = new Label("Levels completed: " + kingdom.getResearch().completedLevels(), skin);
        total.setFontScale(0.65f);
        summaryBox.add(total).left().padTop(4f).row();
    }

    private void say(String message, boolean ok) {
        status.setText(message);
        status.setColor(ok ? Color.GREEN : Color.SALMON);
    }
}

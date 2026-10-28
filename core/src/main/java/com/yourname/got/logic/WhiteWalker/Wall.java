package com.yourname.got.logic.WhiteWalker;

import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;
import com.yourname.got.mediator.GameMediator;
import java.io.Serializable;

public class Wall implements Serializable {
    private static final long serialVersionUID = 6345979683692326859L;

    private long health;
    private final long initialHealth;
    private int lastAttackDay;
    private int totalHelpCount;
    private boolean destroyed;
    private boolean restoredOnce;
    protected final MyLinkedList<ContributionRecord> contributions;

    private long restoreThreshold = 500;

    public Wall(long initialHealth) {
        this.health = Math.max(1, initialHealth);
        this.initialHealth = this.health;
        this.destroyed = false;
        this.restoredOnce = false;
        this.contributions = new MyLinkedList<>();
    }

    public boolean isDestroyed() { return destroyed; }
    public long getHealth() { return health; }
    public long getInitialHealth() { return initialHealth; }
    public int getLastAttackDay() { return lastAttackDay; }
    public int getTotalHelpCount() { return totalHelpCount; }

    public void takeDamage(long damage, int currentDay) {
        if (destroyed) return;
        health -= Math.max(0, damage);
        lastAttackDay = currentDay;
        if (health <= 0) {
            destroyed = true;
            health = 0;
            System.out.println("❌ دیوار نابود شد!");
            GameMediator.sendGlobalMessage("The Wall was destroyed!");
        } else {
            System.out.println("⚔️ دیوار آسیب دید. سلامت فعلی: " + health);
            GameMediator.sendGlobalMessage("The wall was damaged. Current health: "+health);

        }
    }

    public void reinforce(Kingdom k, long amount, int currentDay) {
        if (amount <= 0) return;
        if (destroyed) {
            ContributionRecord rec = findOrCreateRecord(k);
            rec.add(amount);
            totalHelpCount++;
            System.out.println("📦 کمک ثبت شد برای بازسازی: " + amount + " از " + k.getType());
            return;
        }
        health += amount;
        totalHelpCount++;
        ContributionRecord rec = findOrCreateRecord(k);
        rec.add(amount);
        GameMediator.sendGlobalMessage("The Wall was reinforced "+ amount);
        System.out.println("🛡 دیوار تقویت شد +" + amount + " ، سلامت: " + health);
    }

    private ContributionRecord findOrCreateRecord(Kingdom k) {
        Node<ContributionRecord> cur = contributions.getHead();
        while (cur != null) {
            if (cur.data.getKingdom().equals(k)) return cur.data;
            cur = cur.next;
        }
        ContributionRecord r = new ContributionRecord(k, 0);
        contributions.add(r);
        return r;
    }

    public boolean canBeRestored() {
        if (!destroyed || restoredOnce) return false;
        long total = 0;
        Node<ContributionRecord> cur = contributions.getHead();
        while (cur != null) { total += cur.data.getAmount(); cur = cur.next; }
        return total >= restoreThreshold;
    }

    public boolean restore() {
        if (!canBeRestored()) return false;
        this.health = initialHealth;
        this.destroyed = false;
        this.restoredOnce = true;
        this.contributions.clear();
        System.out.println("🧱 دیوار با تلاش جمعی بازسازی شد!");
        return true;
    }

    public void forceCollapseOnFinalDay(int day100) {
        destroyed = true;
        health = 0;
        lastAttackDay = day100;
        System.out.println("💥 دیوار در روز " + day100 + " فرو ریخت (حمله نهایی)!");
    }

    public MyLinkedList<ContributionRecord > getContributions() {
        return this.contributions;
    }

    public void printContributions() {
        for (ContributionRecord c : contributions) {
            System.out.println(c.toString());
        }
    }


    public static class ContributionRecord implements Serializable {
        private static final long serialVersionUID = 2L;
        private final Kingdom kingdom;
        private long amount;
        public ContributionRecord(Kingdom k, long amount) { this.kingdom = k; this.amount = amount; }
        public Kingdom getKingdom() { return kingdom; }
        public long getAmount() { return amount; }
        public void add(long v) { this.amount += Math.max(0, v); }

        @Override
        public String toString() {
            return "Kingdom: " + kingdom.getType()+ " Amount: " + amount;
        }
    }

}

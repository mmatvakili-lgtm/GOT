package com.yourname.got.logic.util;

import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.login.Time;
import com.yourname.got.logic.model.Army.Soldier;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.enums.ResourceType;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.mediator.GameMediator;

import java.io.Serializable;

public class Bazar extends MyLinkedList<Transaction> implements Serializable {
    private static final long serialVersionUID = 7718973367686476635L;

    public Bazar() {
        super();
    }

    public boolean canAfford(GameUser user, Transaction transaction) {
        int woodToPay = 0, stoneToPay = 0, coinToPay = 0, foodToPay = 0;
        for (ResourceItem item : transaction.getResourcesToTake()) {
            switch (item.type) {
                case Wood -> woodToPay += item.amount;
                case Stone -> stoneToPay += item.amount;
                case Coin -> coinToPay += item.amount;
                case Food -> foodToPay += item.amount;
            }
        }
        if (!user.getKingdom().canAffordResources(woodToPay, stoneToPay, coinToPay, foodToPay)) {
            System.out.println("❌ شما منابع کافی برای پرداخت هزینه این معامله را ندارید.");
            return false;
        }
        SoldierUnit requiredSoldiers = transaction.getSoldiersToTake();
        if (requiredSoldiers != null && requiredSoldiers.getCount() > 0) {
            if (!user.getKingdom().getArmyManager().hasEnoughSoldiers(requiredSoldiers.getType(), requiredSoldiers.getCount())) {
                System.out.println("❌ شما " + requiredSoldiers.getCount() + " سرباز از نوع " + requiredSoldiers.getType().name() + " برای این معامله ندارید.");
                return false;
            }
        }
        return true;
    }

    public void sell(Transaction transaction) {
        GameUser seller = transaction.getExchanger();

        int woodToSell = 0, stoneToSell = 0, coinToSell = 0, foodToSell = 0;
        for (ResourceItem item : transaction.getResourcesToGive()) {
            switch (item.type) {
                case Wood -> woodToSell += item.amount;
                case Stone -> stoneToSell += item.amount;
                case Coin -> coinToSell += item.amount;
                case Food -> foodToSell += item.amount;
            }
        }
        seller.getKingdom().detuctResources(woodToSell, stoneToSell, coinToSell, foodToSell);

        if (transaction.getSoldiersToGive() != null) {
            seller.getKingdom().getArmyManager().removeAndPackageSoldiers(
                transaction.getSoldiersToGive().getType(),
                transaction.getSoldiersToGive().getCount()
            );
        }
        this.add(transaction);
        System.out.println("✅ محموله شما با موفقیت در بازار برای فروش ثبت شد.");
    }

    public void checkBazar(int gameSpeed) {
        Node<Transaction> current = getHead();
        while (current != null) {
            Transaction transaction = current.data;
            current = current.next;
            if (Time.gameFormatTimeDay(transaction.sendTime, gameSpeed) > 5) {
                GameMediator.sendGlobalMessage("A deal expired: " + transaction.toString());
                System.out.println("یک معامله منقضی شد: " + transaction.toString());
                returnTransaction(transaction);
            }
        }
    }

    public void returnTransaction(Transaction transaction) {
        GameUser seller = transaction.getExchanger();
        if (seller != null) {
            System.out.println("بازگرداندن آیتم‌های فروش نرفته به " + seller.getUsername() + "...");
            for(ResourceItem item : transaction.getResourcesToGive()) {
                seller.getKingdom().addResources(item.type.name(), item.amount);
            }
            if (transaction.getSoldiersToGive() != null) {
                seller.getKingdom().getArmyManager().addSoldierPackage(transaction.getSoldiersToGive());
            }
        }
        this.remove(transaction);
    }

    public void addSystemTransaction(MyLinkedList<ResourceItem> give, MyLinkedList<ResourceItem> take, SoldierUnit soldiersGive, SoldierUnit soldiersTake) {
        Transaction sysTransaction = new Transaction(null, 0, give, soldiersGive, take, soldiersTake , null);
        this.add(sysTransaction);
        System.out.println("✅ یک محموله سیستمی جدید به بازار اضافه شد.");
    }

    public int size() {
        int count = 0;
        Node<Transaction> current = getHead();
        while (current != null) { count++; current = current.next; }
        return count;
    }

    public void sellForResource(Transaction transaction) {
        GameUser seller = transaction.getExchanger();

        int woodToSell = 0, stoneToSell = 0, coinToSell = 0, foodToSell = 0;
        for (ResourceItem item : transaction.getResourcesToGive()) {
            switch (item.type) {
                case Wood -> woodToSell += item.amount;
                case Stone -> stoneToSell += item.amount;
                case Coin -> coinToSell += item.amount;
                case Food -> foodToSell += item.amount;
            }
        }
        seller.getKingdom().detuctResources(woodToSell, stoneToSell, coinToSell, foodToSell);

        this.add(transaction);
        System.out.println("✅ محموله منابع شما با موفقیت در بازار ثبت شد.");
    }


    public void sellForSoldier(Transaction transaction) {
        this.add(transaction);
        System.out.println("✅ یگان سرباز شما با موفقیت در بازار برای فروش ثبت شد.");
    }
    public boolean isEmpty() { return getHead() == null; }

    public Transaction get(int index) {
        if (index < 0) return null;
        int currentIndex = 0;
        Node<Transaction> current = getHead();
        while (current != null) {
            if (currentIndex == index) return current.data;
            currentIndex++;
            current = current.next;
        }
        return null;
    }

    public int displayTransactions() {
        if (isEmpty()) {
            System.out.println("❗ در حال حاضر هیچ محموله‌ای در بازار وجود ندارد.");
            return 0;
        }
        System.out.println("\n📦 محموله‌های موجود در بازار:");
        int listSize = size();
        for (int i = 0; i < listSize; i++) {
            Transaction transaction = get(i);
            if (transaction != null) {
                System.out.println(i + ". " + transaction.toString());
            }
        }
        return listSize;
    }

    public Transaction getTransactionByIndex(int index) { return get(index); }
    public int getNumberOfTransactions() { return size(); }

    public enum OfferCadence {
        EVERY_10_DAYS(10),
        EVERY_15_DAYS(15),
        EVERY_20_DAYS(20),
        EVERY_SEASON(25);

        public final int days;
        OfferCadence(int days) { this.days = days; }
    }

    private int lastSpawnDayDragonstone = 0;
    private OfferCadence dragonstoneCadence = OfferCadence.EVERY_10_DAYS;

    private int lastSpawnDayGeneric = 0;
    private OfferCadence genericCadence = OfferCadence.EVERY_15_DAYS;

    public void setGenericOfferCadence(OfferCadence cadence) {
        if (cadence != null) this.genericCadence = cadence;
    }

    public void setDragonstoneOfferCadence(OfferCadence cadence) {
        if (cadence != null) this.dragonstoneCadence = cadence;
    }

    public void autoSeedSoldierOffersIfDue(Kingdom kingdom, Time time) {
        if (kingdom == null || time == null) return;
        if (kingdom.getType() != KingdomType.DRAGONSTONE) return;

        if (dragonstoneCadence == null) {
            dragonstoneCadence = OfferCadence.EVERY_10_DAYS;
        }

        int today = time.getDay();
        boolean due;

        if (dragonstoneCadence == OfferCadence.EVERY_SEASON) {
            due = (time.getSeasonDay() == 1) && (lastSpawnDayDragonstone != today);
        } else {
            due = (today - lastSpawnDayDragonstone) >= dragonstoneCadence.days;
        }

        if (due) {
            DragonstoneMercenaryMarket.ensureOffersForDragonstone(kingdom);
            lastSpawnDayDragonstone = today;
        }
    }

    public void autoSeedGenericOffersIfDue(Time time) {
        if (time == null) return;

        if (this.genericCadence == null) {
            this.genericCadence = OfferCadence.EVERY_15_DAYS;
        }

        if (getNumberOfTransactions() >= 12) return;

        int today = time.getDay();
        boolean due = (today - lastSpawnDayGeneric) >= genericCadence.days;

        if (due) {
            System.out.println("... زمان افزودن بسته‌های مزدور عمومی فرا رسیده است ...");
            GenericMercenaryMarket.ensureOffers(this);
            lastSpawnDayGeneric = today;
        }
    }

    public Result listOffer(Transaction transaction) {
        GameUser seller = transaction.getExchanger();
        Kingdom sellerKingdom = seller.getKingdom();

        if (transaction.getResourcesToGive() != null && !transaction.getResourcesToGive().isEmpty()) {
            int wood = 0, stone = 0, coin = 0, food = 0;
            for (ResourceItem item : transaction.getResourcesToGive()) {
                switch (item.type) {
                    case Wood -> wood += item.amount;
                    case Stone -> stone += item.amount;
                    case Coin -> coin += item.amount;
                    case Food -> food += item.amount;
                }
            }
            if (!sellerKingdom.canAffordResources(wood, stone, coin, food)) {
                return new Result(false, "You don't have enough resources to create this offer.");
            }
        }
        if (transaction.getSoldiersToGive() != null && transaction.getSoldiersToGive().getCount() > 0) {
            SoldierUnit soldiersToSell = transaction.getSoldiersToGive();
            if (!sellerKingdom.getArmyManager().hasEnoughSoldiers(soldiersToSell.getType(), soldiersToSell.getCount())) {
                return new Result(false, "You don't have enough " + soldiersToSell.getType().name() + " to sell.");
            }
        }

        if (transaction.getResourcesToGive() != null) {
            for (ResourceItem item : transaction.getResourcesToGive()) {
                sellerKingdom.spendResources(item.type.name(), item.amount);
            }
        }
        if (transaction.getSoldiersToGive() != null) {
            sellerKingdom.getArmyManager().removeAndPackageSoldiers(
                transaction.getSoldiersToGive().getType(),
                transaction.getSoldiersToGive().getCount()
            );
        }

        this.add(transaction);
        return new Result(true, "Your offer has been successfully listed on the market.");
    }
    public Result buy(Transaction transaction, GameUser buyer) {
        if (transaction == null || buyer == null) {
            return new Result(false, "Invalid transaction or buyer.");
        }

        Kingdom buyerKingdom = buyer.getKingdom();
        GameUser seller = transaction.getExchanger();

        if (seller != null && seller.getId() == buyer.getId()) {
            return new Result(false, "You cannot buy your own offer.");
        }
        if (transaction.getOnlyForKingdom() != null && buyer.getKingdom().getType() != transaction.getOnlyForKingdom()) {
            return new Result(false, "This offer is restricted to " + transaction.getOnlyForKingdom().name() + ".");
        }

        int woodPrice = 0, stonePrice = 0, coinPrice = 0, foodPrice = 0;
        for(ResourceItem item : transaction.getResourcesToTake()) {
            switch(item.type) {
                case Wood -> woodPrice += item.amount;
                case Stone -> stonePrice += item.amount;
                case Coin -> coinPrice += item.amount;
                case Food -> foodPrice += item.amount;
            }
        }
        if (!buyerKingdom.canAffordResources(woodPrice, stonePrice, coinPrice, foodPrice)) {
            return new Result(false, "ERROR: You don't have enough resources to afford this offer.");
        }

        buyerKingdom.detuctResources(woodPrice, stonePrice, coinPrice, foodPrice);

        if (seller != null) {
            seller.getKingdom().addResources("WOOD", woodPrice);
            seller.getKingdom().addResources("STONE", stonePrice);
            seller.getKingdom().addResources("COIN", coinPrice);
            seller.getKingdom().addResources("FOOD", foodPrice);
        }

        if (transaction.getResourcesToGive() != null) {
            for (ResourceItem item : transaction.getResourcesToGive()) {
                buyerKingdom.addResources(item.type.name(), item.amount);
            }
        }
        if (transaction.getSoldiersToGive() != null) {
            buyerKingdom.getArmyManager().addSoldierPackage(transaction.getSoldiersToGive());
        }

        this.remove(transaction);
        return new Result(true, "Purchase successful!");
    }
}

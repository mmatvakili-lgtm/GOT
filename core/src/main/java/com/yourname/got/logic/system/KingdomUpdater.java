package com.yourname.got.logic.system;// File: KingdomUpdater.java

import com.yourname.got.logic.Balance.BuildingManager;
import com.yourname.got.logic.model.Building.Barrack;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.Mine;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;
import com.yourname.got.screens.GamePlayScreen;

import java.io.Serializable;

public class KingdomUpdater implements Serializable {

    // >> ADD THIS INNER CLASS <<
    public static class UpdateResult implements Serializable {
        public final MyLinkedList<String> messages;
        public final boolean visualsChanged;

        public UpdateResult(MyLinkedList<String> messages, boolean visualsChanged) {
            this.messages = messages;
            this.visualsChanged = visualsChanged;
        }
    }
    // >> END OF MODIFICATION 1.1 <<


//    public static MyLinkedList<String> updateKingdom(Kingdom kingdom, int gameSpeed) {
//        MyLinkedList<String> messages = new MyLinkedList<>();
//
//        messages.addAll(updateMines(kingdom, gameSpeed));
//        messages.addAll(updateBarracks(kingdom));
//        updateBuildings(kingdom, gameSpeed);
//
//        return messages;
//    }

    // File: KingdomUpdater.java

    // >> CHANGE THIS METHOD'S SIGNATURE AND RETURN STATEMENT <<
    public static UpdateResult updateKingdom(Kingdom kingdom, int gameSpeed) {
        MyLinkedList<String> messages = new MyLinkedList<>();

        messages.addAll(updateMines(kingdom, gameSpeed));
        messages.addAll(updateBarracks(kingdom));

        // Capture the boolean signal from updateBuildings
        boolean hasVisualChanges = updateBuildings(kingdom, gameSpeed);

        // Return both messages and the visual signal
        return new UpdateResult(messages, hasVisualChanges);
    }

    public static int storeResourceInAllStorages(Kingdom kingdom, String type, int amount) {
        Node<Storage> node = kingdom.getStorages().getHead();
        int remaining = amount;

        while (node != null && remaining > 0) {
            if (node.data.getResourceType().equalsIgnoreCase(type)) {
                remaining -= node.data.addResource(remaining);
            }
            node = node.next;
        }
        return amount - remaining;
    }

    public static MyLinkedList<String> updateMines(Kingdom kingdom, int gameSpeed) {
        MyLinkedList<String> messages = new MyLinkedList<>();

        for (Mine mine : kingdom.getMines()) {
            int produced = mine.produceAmount(gameSpeed);
            if (produced > 0) {
                int stored = storeResourceInAllStorages(kingdom, mine.getResourceType(), produced);

                if (stored < produced) {
                    for (Storage storage : kingdom.getStorages()) {
                        if (storage.getResourceType().equalsIgnoreCase(mine.getResourceType())) {
                            if (!storage.hasSentFullNotification()) {
                                String wasteMessage = String.format(
                                    "WARNING: Your %s storage is full. Resources are being wasted.",
                                    mine.getResourceType()
                                );
                                // --- رفع باگ: ارسال پیام به صورت خصوصی ---
                                if (kingdom.getOwner() != null && kingdom.getOwner().getGame() != null) {
                                    kingdom.getOwner().getGame().sendSystemNotificationToUser(kingdom.getOwner(), "Storage Full", wasteMessage);
                                }
                                storage.setFullNotificationSent(true);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return messages; // این آرایه می‌تواند خالی برگردد، چون پیام به سیستم نوتیفیکشن منتقل شد
    }

    public static MyLinkedList<String> updateBarracks(Kingdom kingdom) {
        MyLinkedList<String> allMessages = new MyLinkedList<>();
        for (Building building : kingdom.getBuildings()) {
            if (building instanceof Barrack barrack) {
                allMessages.addAll(barrack.updateTrainingQueue());
            }
        }
        return allMessages;
    }

    // File: KingdomUpdater.java

    // >> CHANGE THIS METHOD TO RETURN BOOLEAN <<
    private static boolean updateBuildings(Kingdom kingdom, int gameSpeed) {
        boolean visualChangeDetected = false;
        for (Building building : kingdom.getBuildings()) {
            // If any building update returns true, we flag it.
            if (BuildingManager.update(building, gameSpeed)) {
                visualChangeDetected = true;
            }
        }
        return visualChangeDetected;
    }

//    private static void updateBuildings(Kingdom kingdom, int gameSpeed) {
//        for (Building building : kingdom.getBuildings()) {
//            BuildingManager.update(building, gameSpeed);
//        }
//    }
}

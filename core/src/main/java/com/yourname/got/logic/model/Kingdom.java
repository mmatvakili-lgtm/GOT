package com.yourname.got.logic.model;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;
import com.yourname.got.logic.model.Building.Camp;


import java.io.Serializable;
import java.time.LocalDateTime;


public class Kingdom implements Serializable {
    private static final long serialVersionUID = 272020211868870129L;
    private KingdomType type;
    private MyLinkedList<Building> buildings;
    private MyLinkedList<Mine> mines;
    private MyLinkedList<Storage> storages;
    private ArmyManager armyManager;
    private MyLinkedList<Mission> activeMissions = new MyLinkedList<>();
    private MyLinkedList<TrainingLogEntry> trainingLogs = new MyLinkedList<>();
    private MyLinkedList<TrainingTask> trainingQueue ;
    private UserSubMap userMap;
    private GameUser owner;
    private int level;
    private LocalDateTime lastSaveTime;
    private WallsOfGate protectiveWall;
    private Position entryPoint;
    private Position portPoint;
    private MyLinkedList<BuildingHistoryEntry> buildingHistory ;
    private Hero hero;
    private int dragonGlass;
    private BattleShip battleShip;
    private int unreadNotifications = 0;
    private transient long lastUpkeepCheckTime = 0;


    public Kingdom(KingdomType type,Map mainMap, int startX, int startY, int width, int height,int x,int y,int xP,int yP) {
        this.type = type;
        entryPoint=new Position(x, y);
        portPoint = new Position(xP,yP);
        userMap=new UserSubMap(mainMap,startX,startY,width,height,entryPoint);
        if (type==KingdomType.IRON_ISLANDS) {
            this.battleShip = new BattleShip();
        } else {
            this.battleShip = null;
        }
        this.level = 1;
        this.lastSaveTime = LocalDateTime.now();
        this.buildings = new MyLinkedList<>();
        this.mines = new MyLinkedList<>();
        this.storages = new MyLinkedList<>();
        this.armyManager = new ArmyManager();
        this.trainingLogs = new MyLinkedList<>();
        this.trainingQueue = new MyLinkedList<>();
        this.buildingHistory = new MyLinkedList<>();

        Castle castle = new Castle(this);
        castle.markAsBuilt();
        userMap.addBuildingInMap(castle,6,6);
        getBuildings().add(castle);

        if (type==KingdomType.DRAGONSTONE){
            this.addDragonGlass(100);
            var hero = this.getHero();
            if (hero != null && !hero.isUnlocked()) {
                hero.unlock();
                System.out.println("🦸‍♂️ Hero " + hero.getName() + " has been unlocked!");
            }
        }

        buildPerimeterWalls(mainMap,startX,startY);
        if (type == KingdomType.IRON_ISLANDS) {
            for (int dx = -1; dx <= 13; dx++) {
                for (int dy = -1; dy <= 13; dy++) {
                    boolean isOutsidePerimeter = (dx == -1 || dx == 13 || dy == -1 || dy == 13);

                    if (isOutsidePerimeter) {
                        int globalX = startX + dx;
                        int globalY = startY + dy;

                        if (globalX < 0 || globalY < 0 || globalX >= mainMap.getMap().length || globalY >= mainMap.getMap()[0].length)
                            continue;

                        Tile tile = mainMap.getMap()[globalX][globalY];
                        tile.setTerrainType(TerrainType.SEA);
                        tile.setObjectOnTile(new ObjectOnTile("Sea"));
                        tile.setEmpty(false);
                    }
                }
            }
            setPort(mainMap, x, y);
            setPort(mainMap, xP, yP);
        } else {
            setPort(mainMap, xP, yP);
            setGate(mainMap,x,y);
        }
    }

    public ArmyManager getArmyManager() {
        return armyManager;
    }
    public MyLinkedList<TrainingLogEntry> getTrainingLogs() {
        return trainingLogs;
    }
    public MyLinkedList<TrainingTask> getTrainingQueue() {
        if (trainingQueue == null) this.trainingQueue = new MyLinkedList<>();
        return trainingQueue;
    }

    public Castle getCastle() {
        for (Building b : buildings) {
            if (b instanceof Castle castle) {
                return castle;
            }
        }
        return null;
    }
    private void buildPerimeterWalls(Map mainMap, int startX, int startY) {
        for (int dx = 0; dx < 13; dx++) {
            for (int dy = 0; dy < 13; dy++) {
                if (dx == 0 || dy == 0 || dx == 12 || dy == 12) { // فقط مرزها
                    int globalX = startX + dx;
                    int globalY = startY + dy;

                    // جلوگیری از جایگزینی Gate و Port
                    if ((globalX == entryPoint.getX() && globalY == entryPoint.getY()) ||
                        (globalX == portPoint.getX() && globalY == portPoint.getY())) {
                        continue; // از این مختصات عبور کن
                    }
                    WallsOfGate wall = new WallsOfGate(this);
                    Tile tile = mainMap.getMap()[globalX][globalY];
                    tile.setObjectOnTile(new ObjectOnTile("WallsOfGate"));
                    tile.setEmpty(false);
                    tile.getObjectOnTile().setBuilding(wall);
                    armyManager.getDefenceManager().DefenseComplex.setWall(wall);
/// //////////////////////////////
                    this.buildings.add(wall); // <<<<<<< این خط را اضافه کنید

                }
            }
        }
    }

    private void setGate(Map mainMap,int x,int y){
        Tile tile = mainMap.getMap()[x][y];
        tile.setObjectOnTile(new ObjectOnTile("Gate"));
        tile.setEmpty(false);
        Gate gate=new Gate(this);
        tile.getObjectOnTile().setBuilding(gate);
        armyManager.getDefenceManager().DefenseComplex.setGate(gate);
        this.buildings.add(gate);


    }
    private void setPort(Map mainMap,int x,int y){
        Tile tile = mainMap.getMap()[x][y];
        tile.setObjectOnTile(new ObjectOnTile("Port"));
        tile.setEmpty(false);
        Port port=new Port(this,true);
        tile.getObjectOnTile().setBuilding(port);
        armyManager.getDefenceManager().DefenseComplex.setPort(port);
        this.buildings.add(port);
        port.status=Status.stable;

    }
    public BattleShip getBattleShip() {
        return this.battleShip;
    }

    public void setRentedBattleShip(BattleShip ship) {
        this.battleShip = ship;
    }

    public void removeRentedShip() {
        if (this.battleShip != null && this.battleShip.isRented) {
            this.battleShip = null;
        }
    }

    public void printMap() {
        System.out.println("=== Kingdom: " + type + " ===");
        for (Building b : buildings) {
            System.out.println(b);
        }
    }

    public void addMine(Mine mine) {
        this.mines.add(mine);
    }

    public void addStorage(Storage storage) {
        this.storages.add(storage);
    }

    public KingdomType getType() {
        return this.type;
    }

    public MyLinkedList<Building> getBuildings() {
        return this.buildings;
    }

    public MyLinkedList<Mine> getMines() {
        return this.mines;
    }

    public MyLinkedList<Storage> getStorages() {
        return this.storages;
    }

    public GameUser getOwner() {
        return owner;
    }

    public int getLevel() {
        return level;
    }

    public LocalDateTime getLastSaveTime() {
        return lastSaveTime;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setOwner(GameUser owner) {
        this.owner = owner;
    }

    public void setLastSaveTime(LocalDateTime time) {
        this.lastSaveTime = time;
    }

    public Position getEntryPoint() {
        return entryPoint;
    }
    public Position getPortPoint() {
        return portPoint;
    }

    public UserSubMap getUserMap() {
        return userMap;
    }

    public void showAllResources() {
        System.out.println("📦 Your Current Resources:");

        String[] types = {"WOOD", "STONE", "FOOD" ,  "COIN"};

        for (String type : types) {
            int amount = 0;
            int capacity = 0;

            Node<Storage> node = storages.getHead();
            while (node != null) {
                Storage s = node.data;
                if (s.getResourceType().equals(type)) {
                    amount += s.getQuantity();
                    capacity += s.getMaxCapacity();
                }
                node = node.next;
            }

            System.out.println("- " + type + ": " + amount + " / " + capacity);
        }
        System.out.println("- DRAGONGLASS: " + dragonGlass);
    }

    public void  addResource(ResourceType resourceType, int  totalGoldReward) {

    }

    public int countBuildingOfType(String buildingName) {
        int count = 0;
        for (Building b : this.getBuildings()) {
            if (b.getName().equalsIgnoreCase(buildingName)) {
                if (b.isActiveBuilding() || b.isFreezeBuilding()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countMinesOfType(String type) {
        int count = 0;
        for (Mine mine : mines) {
            if ((mine.isActiveBuilding()||mine.isFreezeBuilding()) && mine.getResourceType().equalsIgnoreCase(type)) {
                count++;
            }
        }
        return count;
    }

    public boolean spendResources(String type, int amount) {
        Node<Storage> node = storages.getHead();

        if (getTotalResources(type) < amount) {
            System.out.println("⛔ Not enough " + type + "!");
            return false;
        }

        while (amount > 0 && node != null) {
            Storage s = node.data;
            if (s.getResourceType().equalsIgnoreCase(type)) {
                int canUse = Math.min(amount, s.getQuantity());
                s.useResource(canUse);
                amount -= canUse;
            }
            node = node.next;
        }

        return true;
    }
    public boolean addResources(String type, int amount) {
        Node<Storage> node = storages.getHead();
        while (amount > 0 && node != null) {
            Storage s = node.data;
            if (s.getResourceType().equalsIgnoreCase(type)) {
                int add=s.addResource(amount);
                amount -= add;
            }
            node = node.next;
        }

        return true;
    }

public int getTotalSoldierCapacity() {
    int totalCapacity = 0;
    if (this.buildings == null) {
        return 0;
    }
    for (Building b : this.buildings) {
        if (b instanceof Camp && !b.isDestroyed()) {
            totalCapacity += ((Camp) b).getMaxCapacity();
        }
    }
    return totalCapacity;
}

    public void detuctResources(int wood, int stone, int coin , int food) {
        spendResources("Wood", wood);
        spendResources("Stone", stone);
        spendResources("Coin", coin);
        spendResources("Food", food);
    }

    public boolean canAffordResources(int wood, int stone, int coin , int food) {
        return getTotalResources("Wood") >= wood && getTotalResources("Stone") >= stone && getTotalResources("COIN") >= coin && getTotalResources("Food") >= food;
    }

    public int getTotalResources(String type) {
        int totalAvailable = 0;
        for (Storage s : storages) {
            if (s.getResourceType().equalsIgnoreCase(type)) {
                totalAvailable += s.getQuantity();
            }
        }
        return totalAvailable;
    }
    public int getTotalCapacity(String type) {
        int totalCapacity = 0;
        for (Storage s : storages) {
            if (s.getResourceType().equalsIgnoreCase(type)) {
                totalCapacity += s.getMaxCapacity();
            }
        }
        return totalCapacity;
    }
    public Barrack getFirstAvailableBarrack() {
        for (Building b : this.buildings) {
            if (b instanceof Barrack && b.isBuilt()) {
                return (Barrack) b;
            }
        }
        return null;
    }

    public int getTotalFreeCampCapacity() {
        int totalCapacity = 0;
        for (Building b : this.buildings) {
            if (b instanceof Camp camp && camp.isBuilt()) {
                totalCapacity += camp.getFreeCapacity();
            }
        }
        return totalCapacity;
    }

    public int countBuildingsOfType(String buildingType) {
        if (this.buildings == null || buildingType == null || buildingType.isEmpty()) {
            return 0;
        }

        int count = 0;
        String typeToCompare = buildingType.replace(" ", "");

        for (Building b : this.buildings) {
            if (b != null && b.getClass().getSimpleName().equalsIgnoreCase(typeToCompare)) {
                count++;
            }
        }
        return count;
    }

    public int countStoragesOfType(String type) {
        int count = 0;
        for (Storage storage : storages) {
            if ((storage.isActiveBuilding() || storage.isFreezeBuilding()) && storage.getResourceType().equalsIgnoreCase(type)) {
                count++;
            }
        }
        return count;
    }

    public void showBuildingStatuses(int gameSpeed) {
        System.out.println("📋 Building Status Overview: \n");

        System.out.printf("%-15s %-10s %-10s %-12s %-10s %-20s %-20s %-20s\n",
            "🏛️ نام ساختمان", "Level", "X", "Y", "وضعیت", "سلامت", "⏳ ساخت/ارتقاء", "🛠️ تعمیر");

        System.out.println("=".repeat(125));

        for (Building b : buildings) {
            System.out.println(b.getStatusReport(gameSpeed , this.getUserMap()));
        }
    }


    public void addHistory(String action, Building building) {
        if (buildingHistory == null)
            buildingHistory = new MyLinkedList<>();
        BuildingHistoryEntry entry = new BuildingHistoryEntry(action, building.getName(),userMap.globalToLocal(building.getPosition()).getX()+1, userMap.globalToLocal(building.getPosition()).getY()+1);
        buildingHistory.add(entry);
    }

    public void addHistory(String action, DefenseComplex complex) {
        if (buildingHistory == null)
            buildingHistory = new MyLinkedList<>();

        BuildingHistoryEntry entry = new BuildingHistoryEntry(
            action,
            "مجموعه دفاعی",
            complex.getGate() != null ? userMap.globalToLocal(complex.getGate().getPosition()).getX()+1 : -1,
            complex.getGate() != null ? userMap.globalToLocal(complex.getGate().getPosition()).getY()+1 : -1
        );
        buildingHistory.add(entry);
    }

    public Boolean hasPort() {
        Node<Building> current = buildings.getHead();
        while (current != null) {
            if (current.data instanceof Port) {
                return current.data.getStatus() == Status.stable;

            }
            current = current.next;
        }
        return false;
    }

    public void showBuildingHistory() {
        System.out.println("\n📜 تاریخچه ساخت‌وساز:");
        if (buildingHistory == null || buildingHistory.isEmpty()) {
            System.out.println("هیچ فعالیتی ثبت نشده است.");
            return;
        }
        for (BuildingHistoryEntry entry : buildingHistory) {
            System.out.println(entry.getFormattedEntry());
        }
    }

    public Port getPort() {
        if (this.buildings == null) {
            return null;
        }
        for (Building b : this.buildings) {
            if (b instanceof Port) {
                return (Port) b;
            }
        }
        return null;
    }

    public void showTrainingLogs() {
        System.out.println("\n📜 Training Log:");
        if (trainingLogs.isEmpty()) {
            System.out.println("No training has been completed yet.");
            return;
        }

        for (TrainingLogEntry log : trainingLogs) {
            System.out.println(log);
        }
    }
    public Boolean hasCrowTower(){
        Node<Building> current=buildings.getHead();
        while (current!=null){
            if(current.data.getName().equals("Crow Tower")){
                if(current.data.status== Status.stable)
                    return true;
                return false;
            }
            current=current.next;
        }
        return false;
    }
    public Boolean hasTransport(){
        Node<Building> current=buildings.getHead();
        while (current!=null){
            if(current.data.getName().equals("Transport")){
                if(current.data.status== Status.stable)
                    return true;
                return false;
            }
            current=current.next;

        }
        return false;
    }

    public boolean hasSpy() {
        for (SoldierUnit unit : armyManager.getDefenceManager().getDefenders()) {
            if (unit.getType() == SoldierType.SPY && unit.getCount() > 0) {
                return true;
            }
        }

        if (this.getType()==KingdomType.DRAGONSTONE && getHero()!=null) {
            return true;
        }

        for (TrainingTask task : trainingQueue) {
            if (task.getType() == SoldierType.SPY) {
                return true;
            }
        }

        return false;
    }
    public SoldierUnit getSpy(){
        for (SoldierUnit unit : armyManager.getDefenceManager().getDefenders()) {
            Node<Soldier> current=unit.getSoldiers().getHead();
            if (unit.getType() == SoldierType.SPY && unit.getCount() > 0) {
                return unit;
            }
        }
        return null;
    }



    public Hero getHero() {
        if (hero == null) {
            hero = HeroFactory.createHeroFor(this.type);
        }
        return hero;
    }

    public void addDragonGlass(int amount) {
        dragonGlass += amount;
    }


    public void equipSoldiersWithDragonGlass(int amount) {
        int equipped = 0;
        for (SoldierUnit unit : armyManager.getDefenceManager().getDefenders()) {
            Node<Soldier> current = unit.getSoldiers().getHead();
            while (current != null && equipped < amount) {
                Soldier s = current.data;
                if (!s.isEquippedWithDragonGlass()) {
                    s.equipDragonGlass();
                    equipped++;
                }
                current = current.next;
            }
        }
        dragonGlass -= equipped;
        System.out.println("🧪 " + equipped + " سرباز با شیشه اژدها تجهیز شدند.");
    }

    public int processBuildingDestruction(Building destroyedBuilding, Kingdom attackerKingdom) {
        if (destroyedBuilding == null || destroyedBuilding.getStatus() == Status.destroyed) {
            return 0;
        }

        int lootAmount = 0;

        destroyedBuilding.setStatus(Status.destroyed);
        destroyedBuilding.setStrength(0);

        if (destroyedBuilding instanceof Storage) {
            Storage storage = (Storage) destroyedBuilding;
            lootAmount = storage.getQuantity() / 2;
            if (lootAmount > 0) {
                storage.useResource(lootAmount);
                attackerKingdom.addResources(storage.getResourceType(), lootAmount);
                System.out.println("⚔️ مهاجم " + lootAmount + " واحد " + storage.getResourceType() + " غارت کرد.");
            }
        }

        Tile tile = userMap.mainMap.getMap()[destroyedBuilding.getPosition().getX()][destroyedBuilding.getPosition().getY()];
        if (tile != null) {
            tile.setObjectOnTile(new ObjectOnTile("Rubble")); // نمایش آوار
        }

        if (userMap != null && userMap.arrangement != null) {
            userMap.arrangement.delete(destroyedBuilding);
            System.out.println("LOG: ساختمان " + destroyedBuilding.getName() + " از چیدمان نقشه برای مسیریابی حذف شد.");
        }

        addHistory("Destroyed", destroyedBuilding);
        return lootAmount;
    }

    public int getEquippedSoldierCount() {
        int count = 0;
        for (SoldierUnit unit : armyManager.getDefenceManager().getDefenders()) {
            Node<Soldier> current = unit.getSoldiers().getHead();
            while (current != null) {
                if (current.data.isEquippedWithDragonGlass()) {
                    count++;
                }
                current = current.next;
            }
        }
        return count;
    }

    public void unequipAllDragonGlass() {
        for (SoldierUnit unit : armyManager.getDefenceManager().getDefenders()) {
            Node<Soldier> current = unit.getSoldiers().getHead();
            while (current != null) {
                current.data.setEquippedWithDragonGlass(false);
                current = current.next;
            }
        }
    }

    public int getUnreadNotifications() {
        return unreadNotifications;
    }

    public void incrementUnreadNotifications() {
        this.unreadNotifications++;
    }

    public void resetUnreadNotifications() {
        this.unreadNotifications = 0;
    }

    public CrowTower getCrowTower() {
        Node<Building> current = buildings.getHead();
        while (current != null) {
            if (current.data instanceof CrowTower) {
                if (current.data.getStatus() == Status.stable) {
                    return (CrowTower) current.data;
                }
            }
            current = current.next;
        }
        return null;
    }


    public void registerNewBuilding(Building newBuilding) {
        if (this.buildings == null) {
            this.buildings = new MyLinkedList<>();
        }
        this.buildings.add(newBuilding);

        if (newBuilding instanceof Mine) {
            if (this.mines == null) {
                this.mines = new MyLinkedList<>();
            }
            this.mines.add((Mine) newBuilding);
        }

        if (newBuilding instanceof Storage) {
            if (this.storages == null) {
                this.storages = new MyLinkedList<>();
            }
            this.storages.add((Storage) newBuilding);
        }

    }

    public boolean hasDragonGlass() {
        return dragonGlass > 0;
    }

    public int getDragonGlass() {
        return dragonGlass;
    }

    public long getLastUpkeepCheckTime() {
        if (this.lastUpkeepCheckTime == 0) {
            this.lastUpkeepCheckTime = System.currentTimeMillis();
        }
        return lastUpkeepCheckTime;
    }

    public void setLastUpkeepCheckTime(long time) {
        this.lastUpkeepCheckTime = time;
    }
}

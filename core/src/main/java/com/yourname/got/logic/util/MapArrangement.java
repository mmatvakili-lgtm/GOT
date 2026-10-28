package com.yourname.got.logic.util;

import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.WallsOfGate;
import com.yourname.got.logic.model.Building.Gate;

import java.io.Serializable;

public class MapArrangement implements Serializable {
    protected static final long serialVersionUID = 6078340499985815726L;

    private class Node implements Serializable {
        public Building building;
        public Node link;

        public Node(Building building) {
            this.building = building;
            this.link = null;
        }
    }

    private Node front;

    public MapArrangement() {
        front = null;
    }

    public void add(Building building) {
        if (building instanceof WallsOfGate || building instanceof Gate) {
            return;
        }
        Node newNode = new Node(building);
        newNode.link = front;
        front = newNode;
    }

    public void delete(Building building) {
        if (front == null || building == null) return;

        if (front.building.equals(building)) {
            front = front.link;
            return;
        }

        Node current = front;
        while (current.link != null && !current.link.building.equals(building)) {
            current = current.link;
        }

        if (current.link != null) {
            current.link = current.link.link;
        }
    }

    public MyLinkedList<Building> getAllBuildings() {
        MyLinkedList<Building> list = new MyLinkedList<>();
        Node current = front;
        while (current != null) {
            list.add(current.building);
            current = current.link;
        }
        return list;
    }
}

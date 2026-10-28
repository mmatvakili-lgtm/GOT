package com.yourname.got.logic.system;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.util.Node;

import java.io.Serializable;

public class GameUserList implements Serializable {
    private Node<GameUser> head;

    public void add(GameUser user) {
        Node<GameUser> node = new Node<>(user);
        node.next = head;
        head = node;
    }

    public void remove(GameUser user) {
        Node<GameUser> curr = head, prev = null;
        while (curr != null) {
            if (curr.data.equals(user)) {
                if (prev == null) head = curr.next;
                else prev.next = curr.next;
                return;
            }
            prev = curr;
            curr = curr.next;
        }
    }

    public Node<GameUser> getHead() {
        return head;
    }

    public void forEach(UserConsumer consumer) {
        Node<GameUser> node = head;
        while (node != null) {
            consumer.accept(node.data);
            node = node.next;
        }
    }

    public boolean isEmpty() {
        return head == null;
    }

    public GameUser get(int index) {
        if (index < 0) {
            return null;
        }
        Node<GameUser> current = head;
        int currentIndex = 0;
        while (current != null) {
            if (currentIndex == index) {
                return current.data;
            }
            current = current.next;
            currentIndex++;
        }
        return null;
    }


    public interface UserConsumer {
        void accept(GameUser user);
    }
}

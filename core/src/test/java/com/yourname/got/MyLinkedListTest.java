package com.yourname.got;

import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.support.Check;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

/** The custom list is used everywhere (army formation, missions...): its size and tail must never go stale. */
class MyLinkedListTest {

    @Test
    void clearResetsSizeAndTail() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.clear();
        Check.that(list.getSize() == 0, "size is 0 after clear (was " + list.getSize() + ")");
        Check.that(list.isEmpty(), "list is empty after clear");
        Check.that(list.getTail() == null, "tail is null after clear");

        list.add("x");
        list.add("y");
        Check.that(list.getSize() == 2, "a cleared list counts only the new elements (" + list.getSize() + ")");
        Check.that("x".equals(list.get(0)) && "y".equals(list.get(1)), "elements are in insertion order after clear");
        Check.assertAll();
    }

    @Test
    void addKeepsTailPointingAtTheLastElement() {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        for (int i = 0; i < 1000; i++) list.add(i);
        Check.that(list.getSize() == 1000, "1000 elements added");
        Check.that(list.getTail().data == 999, "tail is the last element");
        Check.that(list.get(0) == 0 && list.get(500) == 500 && list.get(999) == 999, "order is preserved");
        Check.assertAll();
    }

    @Test
    void removeFirstKeepsSizeAndTailConsistent() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.removeFirst(); // must not crash on an empty list
        Check.that(list.getSize() == 0, "removeFirst on an empty list does nothing");

        list.add("a");
        list.removeFirst();
        Check.that(list.isEmpty() && list.getSize() == 0 && list.getTail() == null,
            "removing the only element empties the list and the tail");

        list.add("b");
        list.add("c");
        list.removeFirst();
        Check.that(list.getSize() == 1 && "c".equals(list.get(0)), "one element is left after removeFirst");
        list.add("d");
        Check.that(list.getSize() == 2 && "d".equals(list.getTail().data), "appending after removeFirst still works");
        Check.assertAll();
    }

    @Test
    void removeUpdatesTail() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.remove("c");
        Check.that("b".equals(list.getTail().data), "tail moves back when the last element is removed");
        list.add("d");
        Check.that(list.getSize() == 3 && "d".equals(list.get(2)), "append after removing the tail is linked correctly");
        Check.assertAll();
    }

    @Test
    void iteratorFollowsTheIteratorContract() {
        MyLinkedList<String> list = new MyLinkedList<>();
        list.add("a");
        java.util.Iterator<String> it = list.iterator();
        it.next();
        boolean threw = false;
        try {
            it.next();
        } catch (NoSuchElementException expected) {
            threw = true;
        }
        Check.that(threw, "next() past the end throws NoSuchElementException instead of NullPointerException");
        Check.assertAll();
    }
}

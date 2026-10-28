package com.yourname.got.logic.util;

import java.io.Serializable;
import java.util.Iterator;

public class MyLinkedList<T> implements Iterable<T>, Serializable {

    private Node<T> head;
    private Node<T> tail;

    public int size;

    public MyLinkedList() {
        this.head = null;
        this.tail=null;
        this.size = 0;
    }

    public void add(T data) {
        Node<T> node = new Node<>(data);
        if (head == null) {
            this.head = node;
            this.tail = node;
        } else {
            Node<T> current = this.head;
            while (current.next != null)
                current = current.next;
            current.next = node;
            this.tail=node;
        }
        this.size++;
    }
    public int size(){
        return size;
    }
    public void printAll() {
        Node<T> current = this.head;
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }

    public boolean remove(T data) {
        if (head == null) return false;

        // اگر گره‌ای که باید حذف شود، head است
        if (this.head.data.equals(data)) {
            this.head = this.head.next;
            // اگر لیست بعد از حذف خالی شد، tail را هم null کن
            if (this.head == null) {
                this.tail = null;
            }
            size--;
            return true;
        }

        Node<T> current = this.head;
        // به دنبال گره *قبل* از گره‌ای بگرد که باید حذف شود
        while (current.next != null && !current.next.data.equals(data)) {
            current = current.next;
        }

        // اگر گره مورد نظر پیدا شد
        if (current.next != null) {
            // اگر گره‌ای که حذف می‌شود، tail بود، tail را آپدیت کن
            if (current.next == this.tail) {
                this.tail = current;
            }
            current.next = current.next.next;
            size--;
            return true;
        }

        return false;
    }

    public void clear() {
        this.head = null;
    }

    public void addAll(MyLinkedList<T> other) {
        Node<T> current = other.head;
        while (current != null) {
            this.add(current.data);
            current = current.next;
        }
    }

    public Node<T> getHead() {
        return this.head;
    }

    public boolean isEmpty() {
        return this.head == null;
    }

    public int getSize() {
        return this.size;
    }

    public Node<T> getTail() {
        return tail;
    }
    public void removeFirst(){
        this.head=this.head.next;
        this.size--;
    }
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
    // متد get فعلی خود را کاملاً پاک کرده و این کد را به جای آن قرار دهید

    public T get(int index) {
        // ابتدا چک می‌کنیم که ایندکس درخواست شده معتبر است یا نه
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        Node<T> current = head;
        // حلقه دقیقاً به تعداد 'index' بار تکرار می‌شود تا به گره مورد نظر برسد
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        // وقتی به گره درست رسیدیم، دیتای آن را برمی‌گردانیم
        return current.data;
    }
    public void fillArray(T[] array) {
        if (array.length < this.size()) {
            System.out.println("خطا: آرایه ورودی برای جا دادن تمام عناصر کوچک است.");
            return;
        }

        int i = 0;
        Node<T> current = head;
        while (current != null) {
            array[i] = current.data;
            i++;
            current = current.next;
        }
    }
    public void deleteAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        if (index == 0) {
            head = head.next;
            if (head == null) {
                tail = null;
            }
        } else {
            Node<T> prev = head;
            for (int i = 0; i < index - 1; i++) {
                prev = prev.next;
            }

            // اگر گره‌ای که حذف می‌شود، tail بود، tail را آپدیت کن
            if (prev.next == tail) {
                tail = prev;
            }
            prev.next = prev.next.next;
        }
        size--;
    }

    @Override
    public String toString() {
        String allNode = "";
        int counter=0;
        Node<T> current = this.head;
        while (current != null) {
            allNode+="\n"+(counter++)+"-"+current.data;
            current=current.next;
        }
        return allNode;
    }
    public boolean contains(T item) {
        Node<T> current = head;
        while (current != null) {
            if (current.data.equals(item)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }

        int i = 0;
        Object[] result = a;
        for (Node<T> x = (Node<T>) head; x != null; x = x.next) {
            result[i++] = x.data;
        }

        if (a.length > size) {
            a[size] = null;
        }

        return a;
    }
    public void swap(int index1, int index2) {
        if (index1 < 0 || index1 >= size || index2 < 0 || index2 >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds for swap operation.");
        }
        if (index1 == index2) {
            return;
        }
        Node<T> node1 = getNodeAtIndex(index1);
        Node<T> node2 = getNodeAtIndex(index2);

        T tempData = node1.data;
        node1.data = node2.data;
        node2.data = tempData;
    }


    private Node<T> getNodeAtIndex(int index) {
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current;
    }

    /**
     * داده‌ی یک نود در ایندکس مشخص شده را با داده جدید جایگزین می‌کند.
     * @param index ایندکس نودی که باید آپدیت شود.
     * @param data داده جدید.
     */
    public void set(int index, T data) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        // از متد کمکی که قبلاً برای swap ساختیم استفاده می‌کنیم
        Node<T> nodeToUpdate = getNodeAtIndex(index);
        nodeToUpdate.data = data;
    }
}


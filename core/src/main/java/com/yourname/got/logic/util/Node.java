package com.yourname.got.logic.util;

import java.io.Serializable;

public class Node<T> implements Serializable {
        public T data;
        public Node<T> next;

        public Node(T data) {
            this.data = data;
            this.next = null;
        }
    }


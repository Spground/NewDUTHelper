package com.siwe.dutschedule;

import android.test.AndroidTestCase;

import java.util.ArrayList;

/**
 * Created by asus on 2016/5/3.
 */
public class GenTest extends AndroidTestCase {
    public void test1() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        list.clear();

        list.add(5);
        assertEquals(5, (int)list.get(0));
    }
}

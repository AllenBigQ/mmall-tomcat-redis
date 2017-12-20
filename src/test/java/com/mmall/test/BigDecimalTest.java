package com.mmall.test;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Created by Allen
 */
public class BigDecimalTest {
    @Test
    public void test1(){
        System.out.println(0.05+0.01);
    }
    //输出0.060000000000000005
    @Test
    public void test2(){
        BigDecimal b1=new BigDecimal(0.05);
        BigDecimal b2=new BigDecimal(0.01);
        System.out.println(b1.add(b2));
        //输出0.06000000000000000298372437868010820238851010799407958984375
    }
    @Test
    public void test3(){
        BigDecimal b1=new BigDecimal("0.05");
        BigDecimal b2=new BigDecimal("0.01");
        System.out.println(b1.add(b2));
        //输出0.06
    }
}

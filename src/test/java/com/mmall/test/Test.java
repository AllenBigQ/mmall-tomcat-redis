package com.mmall.test;

/**
 * Created by Allen
 */
public class Test {
    public static void main(String[] args) {
        String s = "abcd";
        switch (s){
            case "abc":
                System.out.println("abc!");
                break;
            case "def":
                System.out.println("def!");
                break;
            default:
                System.out.println("no");
        }
    }
}

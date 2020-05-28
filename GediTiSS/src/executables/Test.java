package executables;

import gedi.core.region.GenomicRegion;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Test {
    public static void main(String[] args) {
        try{
            URL url = new URL("https://www.reddit.com/r/tech/top.json?limit=100");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()),8000);

            StringBuilder sb = new StringBuilder();

            int charsRead;
            char[] inputBuffer = new char[100];
            while(true) {
                charsRead = reader.read(inputBuffer);
                if(charsRead < 0) {
                    break;
                }
                if(charsRead > 0) {
                    sb.append(String.copyValueOf(inputBuffer, 0, charsRead));
//                    System.out.println("Value read " + String.copyValueOf(inputBuffer, 0, charsRead));
                }
            }

            reader.close();
            System.err.println(sb.toString());
        } catch(Exception e){
            e.printStackTrace();
        }


        MyClass myClass = new MyClass();
        myClass.Test();
        myClass.Test2();
        Interface1 i1 = new MyClass();
        B b = new B();
        A a = b;

        A a2 = new A();
    }

    public interface Interface1 {
        int Test();
    }

    public static class MyClass implements Interface1 {

        @Override
        public int Test() {
            return 1337;
        }

        public int Test2() {
            return 42;
        }
    }

    public static class A {

    }

    public static class B extends A {

    }
}

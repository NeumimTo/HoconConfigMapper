package example;

import cz.neumimto.configuration.ConfigMapper;

/**
 * Created by NeumimTo.
 */
public class Main {

    private static String ID = "asd";
    public static void main(String[] args) {
        ConfigMapper.init(ID, Main.class.getProtectionDomain());
        //
        //
        //
        ConfigMapper loader = ConfigMapper.get(ID);
        loader.loadClass(Test.class);
    }
}

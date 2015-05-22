package example;

import cz.neumimto.configuration.ConfigMapper;

/**
 * Created by NeumimTo.
 */
public class Main {

    private static String MODID = "asd";
    public static void main(String[] args) {
        ConfigMapper.init(MODID, Main.class.getProtectionDomain());
        //
        //
        //
        ConfigMapper loader = ConfigMapper.get(MODID);
        loader.loadClass(Test.class);
    }
}

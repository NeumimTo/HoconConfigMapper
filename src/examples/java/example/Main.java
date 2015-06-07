package example;

import cz.neumimto.configuration.ConfigMapper;

import java.io.IOException;
import java.util.List;

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
        loader.loadConfiguration(Test.class);
        ExampleEntity ex = new ExampleEntity("name",12345);
        try {
            loader.persist(ex);
            loader.persist(new ExampleEntity("asdf",3458));
        } catch (IllegalAccessException | IOException e) {
            e.printStackTrace();
        }
        List<ExampleEntity> l = loader.loadEntities(ExampleEntity.class);
        System.out.println(l.size());
    }
}

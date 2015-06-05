package example;

import cz.neumimto.configuration.ConfigMapper;

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
        loader.loadClass(Test.class);
        ExampleEntity ex = new ExampleEntity("name",12345);
        loader.persist(ex);
        loader.persist(new ExampleEntity("asdf",3458));
        List<ExampleEntity> l = loader.loadEntities(ExampleEntity.class);
        System.out.println(l.size());
    }
}

package test;

import cz.neumimto.configuration.*;

import java.util.*;

/**
 * Created by NeumimTo.
 *
 * Working dir is replaced by path you specify in init method
 *
 * @see cz.neumimto.configuration.ConfigMapper#loadClass(Class)
 * @see cz.neumimto.configuration.ConfigMapper#init(String, java.nio.file.Path)
 */
@ConfigurationContainer(path = "{WorkingDir}",filename = "example.Test.conf")
public class Test {

    @ConfigValue(name = "Test")
    public static String Teststr = "This is a value of field test";

    @Comment(content = {"second field"})
    @ConfigValue()
    public static Double TESTDOUBLE = 5D;

    @Comment(content = {"collection of strings in a Vector"})
    @ConfigValue(name = "ListOfStrings")
    public static List<String> stringList = new Vector<String>(Arrays.asList("A","B","C"));

    @Comment(content = {"collection of floats in a Set"})
    @ConfigValue(name = "vectorList")
    public static Set<Float> floatList = new HashSet<Float>(Arrays.asList(30F,40F,80F));

    @Comment(content = {"String map"})
    @ConfigValue(name = "strstrmap")
    public static Map<String,String> map = new HashMap<String,String>(){{
        put("AKey1","AValue");
        put("AKey2","beee");
    }};


    @Comment(content = {"String map", "second line", "third line", "nth line"})
    @ConfigValue()
    public static Map<String,Integer> stringIntPairs = new HashMap<String,Integer>(){{
        put("AKey1",5);
        put("AKey2",5000);
    }};

    @ConfigValue()
    public static TestEnum testEnum = TestEnum.A;
}

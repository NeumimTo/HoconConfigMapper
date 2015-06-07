package cz.neumimto.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigMapper {
    private static class CMPair {
        protected Method parse;
        protected Method set;

        private static CMPair create(Method m1, Method m2) {
            CMPair cmPair = new CMPair();
            cmPair.parse = m1;
            cmPair.set = m2;
            return cmPair;
        }
    }

    private static final String LSEPARATOR;
    private static Map<String, ConfigMapper> currents = new ConcurrentHashMap<String, ConfigMapper>();
    public static Map<Class<?>, CMPair> primitiveWrappers = new HashMap<Class<?>, CMPair>() {{
        try {
            Method m = Field.class.getDeclaredMethod("set", Object.class, Object.class);
            put(Byte.class, CMPair.create(Byte.class.getDeclaredMethod("valueOf", String.class), m));
            put(Boolean.class, CMPair.create(Boolean.class.getDeclaredMethod("valueOf", String.class), m));
            put(Integer.class, CMPair.create(Integer.class.getDeclaredMethod("parseInt", String.class), m));
            put(Short.class, CMPair.create(Short.class.getDeclaredMethod("parseShort", String.class), m));
            put(Double.class, CMPair.create(Double.class.getDeclaredMethod("parseDouble", String.class), m));
            put(Float.class, CMPair.create(Float.class.getDeclaredMethod("parseFloat", String.class), m));
            put(Long.class, CMPair.create(Long.class.getDeclaredMethod("parseLong", String.class), m));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }};

    public static Map<Class<?>, CMPair> primitives = new HashMap<Class<?>, CMPair>() {{
        try {
            put(byte.class, CMPair.create(Boolean.class.getDeclaredMethod("valueOf", String.class), Field.class.getDeclaredMethod("setBoolean", Object.class, boolean.class)));
            put(int.class, CMPair.create(Integer.class.getDeclaredMethod("parseInt", String.class), Field.class.getDeclaredMethod("setInt", Object.class, int.class)));
            put(long.class, CMPair.create(Long.class.getDeclaredMethod("parseLong", String.class), Field.class.getDeclaredMethod("setLong", Object.class, long.class)));
            put(float.class, CMPair.create(Float.class.getDeclaredMethod("valueOf", String.class), Field.class.getDeclaredMethod("setFloat", Object.class, float.class)));
            put(short.class, CMPair.create(Short.class.getDeclaredMethod("parseShort", String.class), Field.class.getDeclaredMethod("setShort", Object.class, short.class)));
            put(double.class, CMPair.create(Double.class.getDeclaredMethod("parseDouble", String.class), Field.class.getDeclaredMethod("setDouble", Object.class, double.class)));
            put(boolean.class, CMPair.create(Boolean.class.getDeclaredMethod("parseBoolean", String.class), Field.class.getDeclaredMethod("setBoolean", Object.class, boolean.class)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }};

    static {
        LSEPARATOR = System.getProperty("line.separator");
    }

    public Path path;

    private ConfigMapper(String str, Path path) {
        this.path = path;
        currents.put(str.toLowerCase(), this);
    }

    public static void init(String id, Path workingFolderPath) {
        new ConfigMapper(id, workingFolderPath);
    }

    public static void init(String id, ProtectionDomain protectionDomain) {
        try {
            Path path = new File(protectionDomain.getCodeSource().getLocation().toURI().getPath()).toPath();
            init(id, path);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static ConfigMapper get(String id) {
        return currents.get(id.toLowerCase());
    }

    public <E> List<E> loadEntities(Class<E> clazz) {
        Entity entity = clazz.getAnnotation(Entity.class);
        if (entity == null)
            return Collections.EMPTY_LIST;
        String ext = entity.fileExt();
        return loadEntities(clazz, "*." + ext);
    }


    private File getEntityDir(Entity entity) {
        File dir = null;
        if (entity.directoryPath().trim().equalsIgnoreCase("")) {
            try {
                dir = new File(ConfigMapper.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            dir = new File(entity.directoryPath().replace("{WorkingDir}", path.toString()));
        }
        return dir;
    }


    public <E> E loadEntity(Class<E> clazz, String id) {
        Entity en = clazz.getAnnotation(Entity.class);
        String fname = en.name().replace("{id}", id);
        File dir = getEntityDir(en);
        File f = new File(dir, fname);
        if (!f.exists())
            return null;
        return loadEntity(clazz, f);
    }

    public <E> E loadEntity(Class<E> clazz, File f) {
        E e = null;
        try {
            e = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e1) {
            e1.printStackTrace();
            return null;
        }
        Config c = ConfigFactory.parseFile(f);
        try {
            loadFields(clazz, e, c);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e1) {
            e1.printStackTrace();
        }
        return e;
    }

    public <E> void persist(E e) throws IllegalAccessException, IOException {
        Entity en = e.getClass().getAnnotation(Entity.class);
        String id = null;
        for (Field f : e.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(Entity.Id.class)) {
                //dont modify field access
                Method m = getGetter(e, f);
                try {
                    id = m.invoke(e).toString();
                } catch (InvocationTargetException e1) {
                    e1.printStackTrace();

                }
            }
        }
        if (id == null)
            throw new IllegalStateException("Missing id-field in " + e.getClass());
        File dir = getEntityDir(en);
        String fn = en.name().replace("{id}", id) + en.fileExt();
        File f = new File(dir, fn);
        if (f.exists())
            f.delete();
        writeToFile(f, e, e.getClass());
    }

    private Method getSetter(Object o, Field f) {
        String n = "set" + capitaliziFirst(f.getName());

        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(n)) {
                return m;
            }
        }
        return null;
    }

    private String capitaliziFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public Method getGetter(Object o, Field f) {
        Method m = null;
        try {
            m = o.getClass().getDeclaredMethod("get" + capitaliziFirst(f.getName()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Missing getter of " + o.getClass().getName() + "." + f.getName());
        }
        return m;
    }

    public <E> List<E> loadEntities(Class<E> clazz, String wildcart) {
        Entity entity = clazz.getAnnotation(Entity.class);
        if (entity == null)
            return Collections.EMPTY_LIST;
        File dir = null;
        if (entity.directoryPath().trim().equalsIgnoreCase("")) {
            try {
                dir = new File(ConfigMapper.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            dir = new File(entity.directoryPath().replace("{WorkingDir}", path.toString()));
        }
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + wildcart);
        List<E> entities = new Vector<E>();
        for (File f : dir.listFiles()) {
            if (pathMatcher.matches(f.toPath())) {
                E e = loadEntity(clazz, f);
                if (e != null)
                    entities.add(e);
            }
        }
        return entities;
    }

    private String removeExt(String str) {
        if (str == null)
            return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1)
            return str;
        return str.substring(0, pos);
    }

    private void writeToFile(File file, Object ref, Class<?> clazz) throws IOException {
        FileWriter writer = new FileWriter(file);
        Comment comment = clazz.getAnnotation(Comment.class);
        if (comment != null) {
            writeComments(comment, writer);
        }
        for (Field f : clazz.getDeclaredFields()) {
            ConfigValue value = f.getAnnotation(ConfigValue.class);
            if (value == null) {
                continue;
            }
            comment = f.getAnnotation(Comment.class);
            if (comment != null) {
                writeComments(comment, writer);
            }
            String valueid = value.name();
            if (valueid.trim().equalsIgnoreCase("")) {
                valueid = f.getName();
            }
            String content = " ";
            if (f.isAccessible()) {
                //access field directly
                if (f.getType().isPrimitive() || primitiveWrappers.containsKey(f.getType())) {
                    content = primitiveToString(f, ref);
                } else if (f.getType().isAssignableFrom(String.class)) {
                    content = "\"" + primitiveToString(f, ref) + "\"";
                } else if (Collection.class.isAssignableFrom(f.getType())) {
                    content = collectionToString(f, ref);
                } else if (Map.class.isAssignableFrom(f.getType())) {
                    content = mapToString(f, ref);
                }
            } else {
                //call getter method
                Method m = getGetter(ref, f);
                Class r = m.getReturnType();
                if (String.class.isAssignableFrom(r)) {
                    try {
                        content = "\"" + m.invoke(ref) + "\"";
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } else if (primitives.containsKey(r) || primitiveWrappers.containsKey(r)) {
                    try {
                        content = m.invoke(ref) + "";
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } else if (Collection.class.isAssignableFrom(r)) {
                    //todo
                } else if (Map.class.isAssignableFrom(r)) {
                    //todo
                }
            }
            writer.write(getSerializedNode(valueid) + " : " + content + LSEPARATOR);
        }
        writer.flush();
        writer.close();
    }


    /**
     * This method creates defaults and loads configuration from file.
     * All fields you wish to load must be static and annotated with @ConfigValue
     */
    public void loadConfiguration(Class<?> clazz) {
        ConfigurationContainer container = clazz.getAnnotation(ConfigurationContainer.class);
        if (container == null)
            return;
        String filename = container.filename();
        if (filename.trim().equalsIgnoreCase("")) {
            filename = clazz.getSimpleName();
        }
        File file = null;
        if (container.path().trim().equalsIgnoreCase("")) {
            try {
                file = new File(new File(ConfigMapper.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()), filename);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            file = new File(container.path().replace("{WorkingDir}", path.toString()), filename);
        }
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                writeToFile(file, null, clazz);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        com.typesafe.config.Config config = ConfigFactory.parseFile(file);
        try {
            loadFields(clazz, null, config);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void loadFields(Class<?> clazz, Object ref, Config config) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAccessible()) {
                if (f.getType().isAssignableFrom(String.class)) {
                    f.set(ref, config.getString(getNodeName(f)));
                } else {
                    CMPair cm = getCMPair(f);
                    String nodename = getNodeName(f);
                    if (cm != null) {
                        String s = config.getString(nodename);
                        Object value = cm.parse.invoke(ref, s);
                        Method m = cm.set;
                        m.invoke(f, f, value);
                    } else if (f.getType().isAssignableFrom(List.class)) {
                        f.set(ref, stringToList(ref, f, config.getStringList(nodename), config));
                    } else if (f.getType().isAssignableFrom(Set.class)) {
                        f.set(ref, stringToSet(ref, f, config.getStringList(nodename), config));
                    } else if (f.getType().isAssignableFrom(Map.class)) {
                        f.set(ref, stringToMap(ref, f, config.getConfig(nodename)));
                    } else {
                        IMarshaller<?> marshaller = f.getAnnotation(ConfigValue.class).as().newInstance();
                        Object o = marshaller.unmarshall(config.getConfig(getNodeName(f)));
                        f.set(ref, o);
                    }
                }
            } else {
                Method m = getSetter(ref, f);
                //todo invoker setter
            }
        }
    }

    private CMPair getCMPair(Field f) {
        if (f.getType().isPrimitive())
            return primitives.get(f.getType());
        return primitiveWrappers.get(f.getType());
    }

    private boolean isValidMap(Class<?> key, Class<?> value) {
        if (isWrappedPrimitiveOrString(key) && isWrappedPrimitiveOrString(value)) {
            return true;
        }
        return false;
    }

    private Map<?, ?> stringToMap(Object ref, Field f, Config config) {
        try {
            Map<Object, Object> map = (Map<Object, Object>) f.get(null);
            map.clear();
            ParameterizedType type = (ParameterizedType) f.getGenericType();
            Class<?> key = (Class<?>) type.getActualTypeArguments()[0];
            Class<?> value = (Class<?>) type.getActualTypeArguments()[1];
            if (isValidMap(key, value)) {
                for (Map.Entry<String, com.typesafe.config.ConfigValue> val : config.entrySet()) {
                    Object k = new Object();
                    Object v = new Object();
                    if (key.isAssignableFrom(String.class)) {
                        k = val.getKey();
                    } else {
                        CMPair cm = primitiveWrappers.get(key);
                        k = cm.parse.invoke(ref, val.getKey());
                    }
                    if (value.isAssignableFrom(String.class)) {
                        v = val.getValue().render();
                    } else {
                        CMPair cm = primitiveWrappers.get(value);
                        String input = val.getValue().render();
                        v = cm.parse.invoke(ref, input);
                    }
                    map.put(k, v);
                }
            } else {
                IMapMarshaller<?, ?> mapMarshaller = (IMapMarshaller<?, ?>) f.getAnnotation(ConfigValue.class).as().newInstance();
                Map.Entry entry = mapMarshaller.unmarshall(config);
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getNodeName(Field field) {
        ConfigValue c = field.getAnnotation(ConfigValue.class);
        if (c != null) {
            if (!c.name().equalsIgnoreCase(""))
                return c.name();
        }
        return field.getName();
    }

    private Set<?> stringToSet(Object ref, Field f, List<String> config, Config c) {
        try {
            Set set = (Set<?>) f.get(ref);
            set.clear();
            set.addAll(stringToCollection(ref, f, config, c));
            return set;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new HashSet<Object>();
    }

    private <T extends Number> List<T> strToWrapper(Object ref, Class<T> excepted, List<String> list) {
        List l = new Vector<>();
        CMPair cmPair = primitiveWrappers.get(excepted);
        for (String s : list)
            try {
                l.add(cmPair.parse.invoke(ref, s));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        return l;
    }

    private Collection<?> stringToCollection(Object ref, Field f, List<String> config, Config c) {
        try {
            Collection list = (Collection) f.get(ref);
            list.clear();
            Class<?> type = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
            if (type.isAssignableFrom(String.class)) {
                list = config;
            } else if (type.isAssignableFrom(Double.class)) {
                list = strToWrapper(ref, Double.class, config);
            } else if (type.isAssignableFrom(Integer.class)) {
                list = strToWrapper(ref, Integer.class, config);
            } else if (type.isAssignableFrom(Float.class)) {
                list = strToWrapper(ref, Float.class, config);
            } else if (type.isAssignableFrom(Short.class)) {
                list = strToWrapper(ref, Short.class, config);
            } else {
                IMarshaller<?> marshaller = f.getAnnotation(ConfigValue.class).as().newInstance();
                list.add(marshaller.unmarshall(c.getConfig(getNodeName(f))));
            }
            return list;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<?> stringToList(Object ref, Field f, List<String> config, Config c) {
        List list = (List<?>) getFieldValue(f, ref);
        list.clear();
        list.addAll(stringToCollection(ref, f, config, c));
        return list;
    }

    private String collectionToString(Field f, Object ref) {
        String b = "[ ";
        Class<?> fclass = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
        if (List.class.isAssignableFrom(f.getType()) || Set.class.isAssignableFrom(f.getType())) {
            try {
                if (fclass.isAssignableFrom(String.class)) {
                    for (Object o : (Collection) f.get(ref)) {
                        b += "\"" + o + "\", ";
                    }
                } else if (isWrappedPrimitiveOrString(fclass)) {
                    for (Object o : (Collection) f.get(ref)) {
                        b += o.toString() + ", ";
                    }
                } else {
                    ConfigValue v = f.getAnnotation(ConfigValue.class);
                    try {
                        IMarshaller m = v.as().newInstance();
                        for (Object o : (Collection) f.get(ref)) {
                            b += "\"" + m.marshall(o) + "\", ";
                        }
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            return null;
        }
        b = b.substring(0, b.length() - 2);

        b += " ]";
        return b;
    }

    public Object getFieldValue(Field f, Object ref) {
        if (Modifier.isStatic(f.getModifiers())) {
            try {
                return f.get(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (Modifier.isPublic(f.getModifiers())) {
            try {
                return f.get(ref);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        try {
            return getGetter(ref, f).invoke(ref);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String mapToString(Field f, Object ref) {
        Map<?, ?> map = (Map<?, ?>) getFieldValue(f, ref);
        /*
        try {
            map = (Map) f.get(ref);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }*/
        String a = "{" + LSEPARATOR;
        ParameterizedType type = (ParameterizedType) f.getGenericType();
        Class<?> key = (Class<?>) type.getActualTypeArguments()[0];
        Class<?> value = (Class<?>) type.getActualTypeArguments()[1];
        if (isWrappedPrimitiveOrString(key) && isWrappedPrimitiveOrString(value)) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                a += "\t" + mapEntryToString(entry.getKey()) + " : " + mapEntryToString(entry.getValue()) + "," + LSEPARATOR;
            }
        } else {
            try {
                Class<IMapMarshaller> m = (Class<IMapMarshaller>) f.getAnnotation(ConfigValue.class).as();
                IMarshaller mar = m.newInstance();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    a += mar.marshall(entry) + "," + LSEPARATOR;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        a = a.substring(0, a.length() - (1 + LSEPARATOR.length()));
        return a + LSEPARATOR + "}";
    }


    private boolean isWrappedPrimitiveOrString(Class<?> clazz) {
        if (primitiveWrappers.containsKey(clazz)) {
            return true;
        }
        if (primitives.containsKey(clazz))
            return true;
        if (clazz.isAssignableFrom(String.class)) {
            return true;
        }
        return false;
    }

    private String primitiveToString(Field f, Object ref) {
        if (Modifier.isStatic(f.getModifiers())) {
            try {
                return f.get(ref) + "";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        try {
            return getGetter(ref, f).invoke(ref) + "";
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeComments(Comment comment, FileWriter writer) {
        for (String string : comment.content()) {
            try {
                writer.write("#" + string + LSEPARATOR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String mapEntryToString(Object entry) {
        if (entry.getClass().isAssignableFrom(String.class))
            return "\"" + entry + "\"";
        if (primitiveWrappers.containsKey(entry.getClass())) {
            return entry.toString();
        }
        return null;
    }

    private String getSerializedNode(String nodeValue) {
        if (nodeValue.contains(".")) {
            return nodeValue;
        }

        return "\"" + nodeValue + "\"";
    }
}

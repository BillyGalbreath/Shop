package com.snowgears.shop.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtil {
    private static String versionString;
    private static final Map<String, Class<?>> loadedNMSClasses = new HashMap<>();
    private static final Map<String, Class<?>> loadedOBCClasses = new HashMap<>();
    private static final Map<Class<?>, Map<String, Method>> loadedMethods = new HashMap<>();

    private static String getVersion() {
        if (versionString == null) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            versionString = name.substring(name.lastIndexOf('.') + 1) + ".";
        }
        return versionString;
    }

    private static Class<?> getNMSClass(String nmsClassName) {
        if (loadedNMSClasses.containsKey(nmsClassName)) {
            return loadedNMSClasses.get(nmsClassName);
        }
        String clazzName = "net.minecraft.server." + getVersion() + nmsClassName;
        Class<?> clazz;
        try {
            clazz = Class.forName(clazzName);
        } catch (Throwable t) {
            t.printStackTrace();
            return loadedNMSClasses.put(nmsClassName, null);
        }
        loadedNMSClasses.put(nmsClassName, clazz);
        return clazz;
    }

    private static synchronized Class<?> getOBCClass(String obcClassName) {
        if (loadedOBCClasses.containsKey(obcClassName)) {
            return loadedOBCClasses.get(obcClassName);
        }
        String clazzName = "org.bukkit.craftbukkit." + getVersion() + obcClassName;
        Class<?> clazz;
        try {
            clazz = Class.forName(clazzName);
        } catch (Throwable t) {
            t.printStackTrace();
            loadedOBCClasses.put(obcClassName, null);
            return null;
        }
        loadedOBCClasses.put(obcClassName, clazz);
        return clazz;
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        if (!loadedMethods.containsKey(clazz)) {
            loadedMethods.put(clazz, new HashMap<>());
        }
        Map<String, Method> methods = loadedMethods.get(clazz);
        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        }
        try {
            Method method = clazz.getMethod(methodName, params);
            methods.put(methodName, method);
            loadedMethods.put(clazz, methods);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
            methods.put(methodName, null);
            loadedMethods.put(clazz, methods);
            return null;
        }
    }

    public static String convertItemStackToJson(ItemStack itemStack) {
        try {
            return (String) getMethod(getNMSClass("ItemStack"), "save", getNMSClass("NBTTagCompound")).invoke(
                    getMethod(getOBCClass("inventory.CraftItemStack"), "asNMSCopy", ItemStack.class).invoke(null, itemStack),
                    getNMSClass("NBTTagCompound").newInstance()
            );
        } catch (Throwable t) {
            return "";
        }
    }
}

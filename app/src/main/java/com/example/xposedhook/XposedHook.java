package com.example.xposedhook;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by xlzh on 2017/4/11.
 */

public class XposedHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("com.example.xposedhooktarget")) {
            final Class<?> clazz = XposedHelpers.findClass("com.example.xposedhooktarget.HookDemo", loadPackageParam.classLoader);
            //getClassInfo(clazz);

            //不需要获取类对象，即可直接修改类中的私有静态变量staticInt
            XposedHelpers.setStaticIntField(clazz, "staticInt", 99);

            //Hook无参构造函数，啥也不干。。。。
            XposedHelpers.findAndHookConstructor(clazz, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Haha, HookDemo constructed was hooked" );
                    //大坑，此时对象还没有建立，即不能获取对象，也不能修改非静态变量的值
                    //XposedHelpers.setIntField(param.thisObject, "publicInt", 199);
                    //XposedHelpers.setIntField(param.thisObject, "privateInt", 299);
                }
            });

            //Hook有参构造函数，修改参数
            XposedHelpers.findAndHookConstructor(clazz, String.class,  new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = "Haha, HookDemo(str) are hooked";
                }
            });

            //Hook有参构造函数，修改参数------不能使用XC_MethodReplacement()替换构造函数内容，
            //XposedHelpers.findAndHookConstructor(clazz, String.class, new XC_MethodReplacement() {
            //    @Override
            //    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
            //        Log.d("HookDemo" , "HookDemo(str) was replace");
            //    }
            //});

            //Hook公有方法publicFunc，
            // 1、修改参数
            // 2、修改下publicInt和privateInt的值
            // 3、再顺便调用一下隐藏函数hideFunc
            //XposedHelpers.findAndHookMethod("com.example.xposedhooktarget.HookDemo", clazz.getClassLoader(), "publicFunc", String.class, new XC_MethodHook()
            XposedHelpers.findAndHookMethod(clazz, "publicFunc", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = "Haha, publicFunc are hooked";
                    XposedHelpers.setIntField(param.thisObject, "publicInt", 199);
                    XposedHelpers.setIntField(param.thisObject, "privateInt", 299);
                    // 让hook的对象本身去执行流程
                    Method md = clazz.getDeclaredMethod("hideFunc", String.class);
                    md.setAccessible(true);
                    //md.invoke(param.thisObject, "Haha, hideFunc was hooked");
                    XposedHelpers.callMethod(param.thisObject, "hideFunc", "Haha, hideFunc was hooked");

                    //实例化对象，然后再调用HideFunc方法
                    //Constructor constructor = clazz.getConstructor();
                    //XposedHelpers.callMethod(constructor.newInstance(), "hideFunc", "Haha, hideFunc was hooked");
                }
            });

            //Hook私有方法privateFunc，修改参数
            //XposedHelpers.findAndHookMethod("com.example.xposedhooktarget.HookDemo", clazz.getClassLoader(), "privateFunc", String.class, new XC_MethodHook()
            XposedHelpers.findAndHookMethod(clazz, "privateFunc", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = "Haha, privateFunc are hooked";
                }
            });

            //Hook私有方法repleaceFunc, 替换打印内容
            XposedHelpers.findAndHookMethod(clazz, "repleaceFunc", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Log.d("HookDemo", "Haha, repleaceFunc are replaced");
                    return null;
                }
            });
            //Hook方法, anonymousInner， 参数是抽象类，先加载所需要的类即可
            Class animalClazz  = loadPackageParam.classLoader.loadClass("com.example.xposedhooktarget.Animal");
            XposedHelpers.findAndHookMethod(clazz, "anonymousInner", animalClazz, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("HookDemo This is test");
                    param.args[1] = "Haha, anonymousInner are hooked";
                }
            });

            //Hook匿名类的eatFunc方法，修改参数，顺便修改类中的anonymoutInt变量
            XposedHelpers.findAndHookMethod("com.example.xposedhooktarget.HookDemo$1", clazz.getClassLoader(),
                    "eatFunc", String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.args[0] = "Haha, eatFunc are hooked";
                            XposedHelpers.setIntField(param.thisObject, "anonymoutInt", 499);
                        }
                    });

            //hook内部类的构造方法失败，且会导致hook内部类的InnerFunc方法也失败，原因不明
//            XposedHelpers.findAndHookConstructor(clazz1, new XC_MethodHook() {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            XposedBridge.log("Haha, InnerClass constructed was hooked" );
//                        }
//                    });

            //Hook内部类InnerClass的InnerFunc方法，修改参数，顺便修改类中的innerPublicInt和innerPrivateInt变量
            final Class<?> clazz1 = XposedHelpers.findClass("com.example.xposedhooktarget.HookDemo$InnerClass", loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(clazz1, "InnerFunc", String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.args[0] = "Haha, InnerFunc was hooked";
                            XposedHelpers.setIntField(param.thisObject, "innerPublicInt", 9);
                            XposedHelpers.setIntField(param.thisObject, "innerPrivateInt", 19);
                        }
                    });
        }
    }

    private void getClassInfo(Class clazz) {
        //getFields()与getDeclaredFields()区别:getFields()只能访问类中声明为公有的字段,私有的字段它无法访问，
        //能访问从其它类继承来的公有方法.getDeclaredFields()能访问类中所有的字段,与public,private,protect无关，
        //不能访问从其它类继承来的方法
        //getMethods()与getDeclaredMethods()区别:getMethods()只能访问类中声明为公有的方法,私有的方法它无法访问,
        //能访问从其它类继承来的公有方法.getDeclaredFields()能访问类中所有的字段,与public,private,protect无关,
        //不能访问从其它类继承来的方法
        //getConstructors()与getDeclaredConstructors()区别:getConstructors()只能访问类中声明为public的构造函数
        //getDeclaredConstructors()能访问类中所有的构造函数,与public,private,protect无关

        //XposedHelpers.setStaticObjectField(clazz,"sMoney",110);
        //Field sMoney = clazz.getDeclaredField("sMoney");
        //sMoney.setAccessible(true);
        Field[] fs;
        Method[] md;
        Constructor[] cl;
        fs = clazz.getFields();
        for (int i = 0; i < fs.length; i++) {
            XposedBridge.log("HookDemo getFiled: " + Modifier.toString(fs[i].getModifiers()) + " " +
                    fs[i].getType().getName() + " " + fs[i].getName());
        }
        fs = clazz.getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            XposedBridge.log("HookDemo getDeclaredFields: " + Modifier.toString(fs[i].getModifiers()) + " " +
                    fs[i].getType().getName() + " " + fs[i].getName());
        }
        md = clazz.getMethods();
        for (int i = 0; i < md.length; i++) {
            Class<?> returnType = md[i].getReturnType();
            XposedBridge.log("HookDemo getMethods: " + Modifier.toString(md[i].getModifiers()) + " " +
                    returnType.getName() + " " + md[i].getName());
            //获取参数
            //Class<?> para[] = md[i].getParameterTypes();
            //for (int j = 0; j < para.length; ++j) {
            //System.out.print(para[j].getName() + " " + "arg" + j);
            //if (j < para.length - 1) {
            //    System.out.print(",");
            //}
            //}
        }
        md = clazz.getDeclaredMethods();
        for (int i = 0; i < md.length; i++) {
            Class<?> returnType = md[i].getReturnType();
            XposedBridge.log("HookDemo getDeclaredMethods: " + Modifier.toString(md[i].getModifiers()) + " " +
                    returnType.getName() + " " + md[i].getName());
        }
        cl = clazz.getConstructors();
        for (int i = 0; i < cl.length; i++) {
            XposedBridge.log("HookDemo getConstructors: " + Modifier.toString(cl[i].getModifiers()) + " " +
                    md[i].getName());
        }
        cl = clazz.getDeclaredConstructors();
        for (int i = 0; i < cl.length; i++) {
            XposedBridge.log("HookDemo getDeclaredConstructors: " + Modifier.toString(cl[i].getModifiers()) + " " +
                    md[i].getName());
        }
    }
}

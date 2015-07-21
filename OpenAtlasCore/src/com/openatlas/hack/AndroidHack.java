/**
 *  OpenAtlasForAndroid Project
 *  The MIT License (MIT)
 *  Copyright (c) 2015 Bunny Blue
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *  and associated documentation files (the "Software"), to deal in the Software
 *  without restriction, including without limitation the rights to use, copy, modify,
 *  merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies
 *  or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 *  PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 *  FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *  @author BunnyBlue
 * **/
package com.openatlas.hack;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.openatlas.hack.Hack.HackDeclaration.HackAssertionException;
import com.openatlas.runtime.DelegateClassLoader;
import com.openatlas.runtime.DelegateResources;
import com.openatlas.runtime.RuntimeVariables;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;


public class AndroidHack {
    private static Object _mLoadedApk = null;
    private static Object _sActivityThread = null;

    static final class HandlerHack implements Callback {

        private static final int RECEIVER = 113;

        final Object activityThread;
        final Handler handler;

        HandlerHack(Handler handler, Object obj) {
            this.handler = handler;
            this.activityThread = obj;
        }

        @Override
        public boolean handleMessage(Message message) {
            try {
                AndroidHack.ensureLoadedApk();
                handler.handleMessage(message);
                AndroidHack.ensureLoadedApk();
            } catch (Throwable th) {
                th.printStackTrace();

                RuntimeException runtimeException;
                if ((th instanceof ClassNotFoundException) || th.toString().contains("ClassNotFoundException")) {
                    if (message.what != RECEIVER) {
                        String packageName = RuntimeVariables.androidApplication.getPackageName();
                        Object loadedApk = AndroidHack.getLoadedApk(activityThread, packageName);
                        if (loadedApk == null) {
                            runtimeException = new RuntimeException("loadedapk is null");
                        } else {
                            ClassLoader classLoader = OpenAtlasHacks.LoadedApk_mClassLoader.get(loadedApk);
                            if (classLoader instanceof DelegateClassLoader) {
                                runtimeException = new RuntimeException("From Atlas:classNotFound ---", th);
                            } else {
                                String exceptionMessage = "wrong classloader in loadedapk---" + classLoader.getClass().getName();
                                runtimeException = new RuntimeException(exceptionMessage, th);
                            }
                        }
                    }
                } else if ((th instanceof ClassCastException) || th.toString().contains("ClassCastException")) {
                    Process.killProcess(Process.myPid());
                } else {
                    runtimeException = new RuntimeException(th);
                }

                //report exception.
            }
            return true;
        }
    }

    static class ActvityThreadGetter implements Runnable {

        @Override
        public void run() {
            try {
                AndroidHack._sActivityThread = OpenAtlasHacks.ActivityThread_currentActivityThread.invoke(
                        OpenAtlasHacks.ActivityThread.getmClass());
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (OpenAtlasHacks.ActivityThread_currentActivityThread) {
                OpenAtlasHacks.ActivityThread_currentActivityThread.notify();
            }
        }
    }

    public static Object getActivityThread() throws Exception {
        if (_sActivityThread == null) {
            if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
                _sActivityThread = OpenAtlasHacks.ActivityThread_currentActivityThread.invoke(OpenAtlasHacks.ActivityThread.getmClass());
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                synchronized (OpenAtlasHacks.ActivityThread_currentActivityThread) {
                    handler.post(new ActvityThreadGetter());
                    OpenAtlasHacks.ActivityThread_currentActivityThread.wait();
                }
            }
        }
        return _sActivityThread;
    }

    public static Handler hackH() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }

        try {
            Handler handler = (Handler) OpenAtlasHacks.ActivityThread
                    .field("mH")
                    .ofType(Hack.into("android.app.ActivityThread$H").getmClass())
                    .get(activityThread);

            Field declaredField = Handler.class.getDeclaredField("mCallback");
            declaredField.setAccessible(true);
            declaredField.set(handler, new HandlerHack(handler,activityThread));
        } catch (HackAssertionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ensureLoadedApk() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }

        Object loadedApk = getLoadedApk(activityThread, RuntimeVariables.androidApplication.getPackageName());

        if (loadedApk == null) {
            loadedApk = createNewLoadedApk(RuntimeVariables.androidApplication, activityThread);
            if (loadedApk == null) {
                throw new RuntimeException("can't create loadedApk");
            }
        }
        activityThread = loadedApk;
        if (!((OpenAtlasHacks.LoadedApk_mClassLoader.get(activityThread)) instanceof DelegateClassLoader)) {
            OpenAtlasHacks.LoadedApk_mClassLoader.set(activityThread, RuntimeVariables.delegateClassLoader);
            OpenAtlasHacks.LoadedApk_mResources.set(activityThread, RuntimeVariables.getDelegateResources());
        }
    }

    public static Object getLoadedApk(Object obj, String packageName) {
        WeakReference weakReference = (WeakReference) ((Map) OpenAtlasHacks.ActivityThread_mPackages.get(obj)).get(packageName);
        if (weakReference == null || weakReference.get() == null) {
            return null;
        }

        _mLoadedApk = weakReference.get();
        return _mLoadedApk;
    }

    public static Object createNewLoadedApk(Application application, Object obj) {
        try {
            int flag = PackageManager.GET_META_DATA | PackageManager.GET_SHARED_LIBRARY_FILES;

            ApplicationInfo applicationInfo = application.getPackageManager().getApplicationInfo(application.getPackageName(), flag);
            application.getPackageManager();

            Method declaredMethod;
            Resources resources = application.getResources();
            if (resources instanceof DelegateResources) {
                declaredMethod = resources.getClass().getSuperclass().getDeclaredMethod("getCompatibilityInfo");
            } else {
                declaredMethod = resources.getClass().getDeclaredMethod("getCompatibilityInfo");
            }

            declaredMethod.setAccessible(true);
            Class cls = Class.forName("android.content.res.CompatibilityInfo");
            Object invoke = declaredMethod.invoke(application.getResources());
            Method declaredMethod2 = OpenAtlasHacks.ActivityThread.getmClass().getDeclaredMethod("getPackageInfoNoCheck",
                    ApplicationInfo.class, cls);

            declaredMethod2.setAccessible(true);
            invoke = declaredMethod2.invoke(obj, applicationInfo, invoke);
            _mLoadedApk = invoke;
            return invoke;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void injectClassLoader(String str, ClassLoader classLoader) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }
        Object loadedApk = getLoadedApk(activityThread, str);
        if (loadedApk == null) {
            loadedApk = createNewLoadedApk(RuntimeVariables.androidApplication, activityThread);
        }

        if (loadedApk == null) {
            throw new Exception("Failed to get ActivityThread.mLoadedApk");
        }
        OpenAtlasHacks.LoadedApk_mClassLoader.set(loadedApk, classLoader);
    }

    public static void injectApplication(Application application) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }

        Object loadedApk = getLoadedApk(activityThread, application.getPackageName());
        if (loadedApk == null) {
            throw new Exception("Failed to get ActivityThread.mLoadedApk");
        }

        OpenAtlasHacks.LoadedApk_mApplication.set(loadedApk, application);
        OpenAtlasHacks.ActivityThread_mInitialApplication.set(activityThread, application);
    }

    public static void injectResources(Application application, Resources resources) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }

        Object loadedApk = getLoadedApk(activityThread, application.getPackageName());
        if (loadedApk == null) {
            activityThread = createNewLoadedApk(application, activityThread);
            if (activityThread == null) {
                throw new RuntimeException("Failed to get ActivityThread.mLoadedApk");
            }

            if (!((OpenAtlasHacks.LoadedApk_mClassLoader.get(activityThread)) instanceof DelegateClassLoader)) {
                OpenAtlasHacks.LoadedApk_mClassLoader.set(activityThread, RuntimeVariables.delegateClassLoader);
            }
            loadedApk = activityThread;
        }

        OpenAtlasHacks.LoadedApk_mResources.set(loadedApk, resources);
        OpenAtlasHacks.ContextImpl_mResources.set(application.getBaseContext(), resources);
        OpenAtlasHacks.ContextImpl_mTheme.set(application.getBaseContext(), null);
    }

    public static Instrumentation getInstrumentation() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread != null) {
            return OpenAtlasHacks.ActivityThread_mInstrumentation.get(activityThread);
        }
        throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
    }

    public static void injectInstrumentationHook(Instrumentation instrumentation) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }
        OpenAtlasHacks.ActivityThread_mInstrumentation.set(activityThread, instrumentation);
    }

    public static void injectContextHook(ContextWrapper contextWrapper, ContextWrapper contextWrapper2) {
        OpenAtlasHacks.ContextWrapper_mBase.set(contextWrapper, contextWrapper2);
    }
}

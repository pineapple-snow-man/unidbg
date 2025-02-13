package com.dewu;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.Module;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.*;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class dewuNew extends AbstractJni implements IOResolver {
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open: " + pathname);
        if (pathname.equals("proc/sys/kernel/random/boot")) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        }
        if (pathname.equals("/proc/cpuinfo")) {
            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/dewu/files/cpuinfo"), pathname));
        }
        if (pathname.equals("/proc/version")) {
            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/dewu/files/version"), pathname));
        }
        return null;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final DvmClass dvmClass;

    dewuNew() {
        AndroidEmulatorBuilder builder = new AndroidEmulatorBuilder(false) {
            @Override
            public AndroidEmulator build() {
                return new AndroidARMEmulator(processName, rootDir, backendFactories) {
                    @Override
                    protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
                        return new deWuSyscallHandler(svcMemory);
                    }
                };
            }
        };
        emulator = builder
                .setRootDir(new File("unidbg-android/src/test/resources/dewu/rootfs"))
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/dewu/4.94.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().addIOResolver(this);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        emulator.getBackend().registerEmuCountHook(100000);

        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator);
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {
            @Override
            public String getProperty(String key) {
                System.out.println("lilac systemkey:" + key);
                switch (key) {
                    case "net.hostname": {
                        return "MIX2S-zhongeryayadeM";
                    }
                    case "ro.serialno": {
                        return "f8a995f5";
                    }
                    case "ro.boot.serialno": {
                        return "f8a995f5";
                    }
                    case "ro.product.brand": {
                        return "Xiaomi";
                    }
                    case "ro.product.manufacturer": {
                        return "Xiaomi";
                    }
                    case "ro.product.model": {
                        return "MIX 2S";
                    }
                    case "ro.product.cpu.abi": {
                        return "arm64-v8a";
                    }
                    case "ro.product.cpu.abilist": {
                        return "arm64-v8a,armeabi-v7a,armeabi";
                    }
                    case "ro.boot.vbmeta.digest": {
                        return null;
                    }
                    case "init.svc.droid4x": {
                        return null;
                    }
                }
                return "";
            }
        });

        memory.addHookListener(systemPropertyHook);
        DalvikModule dm = vm.loadLibrary("szstone", true);
        module = dm.getModule();
        dm.callJNI_OnLoad(emulator);
        dvmClass = vm.resolveClass("com.shizhuang.stone.main.SzSdk");
    }

    public void call_fun() {
        String a1 = "awt0bapt/data/user/0/com.shizhuang.duapp/filesbav4.94.0bavn486bcndewubc";
        int a2 = 1;
        int a3 = 0;
        dvmClass.callStaticJniMethodObject(emulator, "lf(Ljava/lang/String;II)[B", a1, a2, a3);
    }


    public static void main(String[] args) {
        dewuNew dewuNew = new dewuNew();
        dewuNew.hookPopen();
        dewuNew.call_fun();
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "android/app/ActivityThread->currentActivityThread()Landroid/app/ActivityThread;": {
                return dvmClass.newObject(null);
            }
            case "android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;": {
                String name = varArg.getObjectArg(1).getValue().toString();
                switch (name) {
                    case "android_id": {
                        return new StringObject(vm, "07916307a127cdf0");
                    }
                }
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/app/ActivityThread->getApplication()Landroid/app/Application;": {
                return vm.resolveClass("android/app/Application").newObject(null);
            }
            case "android/app/Application->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
            case "android/app/Application->getContentResolver()Landroid/content/ContentResolver;": {
                return vm.resolveClass("android/content/ContentResolver").newObject(null);
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }


    public void hookPopen() {
        IxHook xHook = XHookImpl.getInstance(emulator);
        xHook.register("libszstone.so", "popen", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                RegisterContext registerContext = emulator.getContext();
                String command = registerContext.getPointerArg(0).getString(0);
                emulator.set("command", command);
                System.out.println("command:" + command);
                return HookStatus.RET(emulator, originFunction);
            }
        }, true);
        xHook.refresh();
    }



}



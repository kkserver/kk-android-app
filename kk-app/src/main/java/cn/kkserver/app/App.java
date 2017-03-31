package cn.kkserver.app;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.kkserver.http.lua.LuaHttp;
import cn.kkserver.lua.LuaState;
import cn.kkserver.obs.lua.LuaObserver;
import cn.kkserver.observer.IObserver;

/**
 * Created by zhanghailong on 2017/3/6.
 */

public class App extends Application {

    public final static String TAG = "kk-app";

    public final static Charset UTF8 = Charset.forName("utf-8");
    public final static int BUFFER_SIZE = 204800;

    private final LuaState _L;

    public App() {

        _L = new LuaState();

        _L.openlibs();

        _L.pushfunction(new WeakLuaFunction<App>(this) {

            @Override
            public int invoke(LuaState L) {

                int top = L.gettop();

                StringBuilder sb = new StringBuilder();

                for(int i=0;i<top;i++) {
                    String v = L.tostring(-top + i);
                    if(v != null) {
                        sb.append(v).append(" ");
                    }
                }

                Log.d(TAG,sb.toString());

                return 0;
            }
        });

        _L.setglobal("print");

        _L.newtable();

        _L.pushstring("getContent");
        _L.pushfunction(new WeakLuaFunction<App>(this){
            @Override
            public int invoke(LuaState L) {

                App app = weakObject();

                if(app != null) {

                    int top = L.gettop();

                    if(top >0) {

                        try {
                            String v = app.getContent(L.tostring( - top));
                            L.pushstring(v);
                            L.pushnil();
                            return 2;
                        } catch (IOException e) {
                            L.pushnil();
                            L.pushstring(e.getMessage());
                            Log.d(TAG,Log.getStackTraceString(e));
                            return 2;
                        }

                    }
                }

                L.pushnil();
                L.pushstring("Not Found");

                return 2;
            }
        });
        _L.rawset(-3);

        _L.pushstring("loadLibrary");
        _L.pushfunction(new WeakLuaFunction<App>(this){
            @Override
            public int invoke(LuaState L) {

                App app = weakObject();

                if(app != null) {

                    int top = L.gettop();

                    if(top >0) {

                        try {
                            app.loadLibrary(L.tostring( - top));
                            L.pushnil();
                            return 1;
                        } catch (Exception e) {
                            L.pushstring(e.getMessage());
                            Log.d(TAG,Log.getStackTraceString(e));
                            return 1;
                        }

                    }
                }

                L.pushstring("Not Found");

                return 1;
            }
        });
        _L.rawset(-3);

        _L.pushstring("document");
        _L.pushstring(getDir("document",0777).getAbsolutePath());
        _L.rawset(-3);

        _L.setglobal("app");


        _L.newtable();

        _L.pushstring("id");

        {
            String szImei = "";
            String m_szDevIDShort = "";
            String m_szAndroidID = "";
            String m_szWLANMAC = "";
            String m_szBTMAC = "";

            try {
                TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
                szImei = TelephonyMgr.getDeviceId();
            } catch (Throwable e){}

            m_szDevIDShort = "35" + //we make this look like a valid IMEI
                    Build.BOARD.length()%10 +
                    Build.BRAND.length()%10 +
                    Build.DEVICE.length()%10 +
                    Build.DISPLAY.length()%10 +
                    Build.HOST.length()%10 +
                    Build.ID.length()%10 +
                    Build.MANUFACTURER.length()%10 +
                    Build.MODEL.length()%10 +
                    Build.PRODUCT.length()%10 +
                    Build.TAGS.length()%10 +
                    Build.TYPE.length()%10 +
                    Build.USER.length()%10 ; //13 digits

            try {
                m_szAndroidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch(Throwable e){}

            try {
                WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                m_szWLANMAC = wm.getConnectionInfo().getMacAddress();
            } catch(Throwable e){}

            try {
                BluetoothAdapter m_BluetoothAdapter = null; // Local Bluetooth adapter
                m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                m_szBTMAC = m_BluetoothAdapter.getAddress();
            } catch (Throwable e){}

            String m_szLongID = szImei + m_szDevIDShort
                    + m_szAndroidID+ m_szWLANMAC + m_szBTMAC;

            MessageDigest m = null;
            try {
                m = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                Log.d(TAG,Log.getStackTraceString(e));
            }
            m.update(m_szLongID.getBytes(),0,m_szLongID.length());
            byte p_md5Data[] = m.digest();
            String m_szUniqueID = new String();
            for (int i=0;i<p_md5Data.length;i++) {
                int b =  (0xFF & p_md5Data[i]);
                if (b <= 0xF)
                    m_szUniqueID+="0";
                m_szUniqueID+=Integer.toHexString(b);
            }
            _L.pushstring(m_szUniqueID);
        }
        _L.rawset(-3);

        _L.pushstring("name");
        _L.pushstring(Build.DISPLAY);
        _L.rawset(-3);

        _L.pushstring("model");
        _L.pushstring(Build.MODEL);
        _L.rawset(-3);

        _L.pushstring("systemName");
        _L.pushstring("Android");
        _L.rawset(-3);

        _L.pushstring("systemVersion");
        _L.pushstring(Build.VERSION.RELEASE);
        _L.rawset(-3);

        _L.setglobal("device");

        {
            //http
            _L.newtable();

            _L.pushstring("__index");
            _L.pushobject(new LuaHttp());
            _L.rawset(-3);

            _L.setglobal("http");
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onTerminate() {
        _L.gc();
        super.onTerminate();
    }

    public void loadObserver(String uri,IObserver obs) throws IOException,LuaException {

        if(_L.loadstring(getContent(uri)) != 0 ) {
            String errmsg = _L.tostring(-1);
            _L.pop(1);
            throw new LuaException(errmsg);
        }

        if (_L.type(-1) == LuaState.LUA_TFUNCTION) {

            _L.pushobject(new LuaObserver(obs));

            if( _L.pcall(1,0) != 0){
                String errmsg = _L.tostring(-1);
                _L.pop(1);
                throw new LuaException(errmsg);
            }

        }
        else{
            _L.pop(1);
        }

    }

    public LuaState L() {
        return _L;
    }

    public String getContent(InputStream in) throws IOException {
        InputStreamReader rd = new InputStreamReader(in,UTF8);
        StringBuffer sb = new StringBuffer();
        char[] data = new char[204800];
        int length;
        while((length =rd.read(data)) > 0) {
            sb.append(data,0,length);
        }
        return sb.toString();
    }

    public String getContent(String uri) throws IOException {

        if(uri.startsWith("assets://")) {
            AssetManager asset = getAssets();
            InputStream in = asset.open(uri.substring(9));
            try {
                return getContent(in);
            } finally {
                in.close();
            }

        } else if(uri.startsWith("class://")) {
            int i = uri.indexOf("/",8);
            String className = "";
            String name = "";
            if(i >0 ){
                className = uri.substring(8,i);
                name = uri.substring(i + 1);
            }
            else {
                className = uri.substring(8);
            }
            try {
                Class<?> clazz = getClassLoader().loadClass(className);
                InputStream in = clazz.getResourceAsStream(name);
                try {
                    return getContent(in);
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        } else {
            InputStream in = new FileInputStream(new File(uri));
            try {
                return getContent(in);
            }
            finally {
                in.close();
            }
        }

    }

    public void loadString(String luaString) throws LuaException{

        if(_L.loadstring(luaString) != 0 ) {
            String errmsg = _L.tostring(-1);
            _L.pop(1);
            throw new LuaException(errmsg);
        }

        if (_L.type(-1) == LuaState.LUA_TFUNCTION) {

            if( _L.pcall(0,0) != 0){
                String errmsg = _L.tostring(-1);
                _L.pop(1);
                throw new LuaException(errmsg);
            }

        }
        else{
            _L.pop(1);
        }
    }

    public void loadLibrary(String uri) throws LuaException,IOException {
        loadString(getContent(uri));
    }

}

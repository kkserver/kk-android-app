package cn.kkserver.app;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

    private LuaState _L;

    @Override
    public void onCreate() {
        super.onCreate();

        _L = new LuaState();

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

        _L.setglobal("app");

    }

    @Override
    public void onTerminate() {
        _L = null;
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

    public void loadContent(String uri) throws LuaException,IOException {
        loadString(getContent(uri));
    }

}

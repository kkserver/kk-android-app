package cn.kkserver.app;

import java.lang.ref.WeakReference;

import cn.kkserver.lua.LuaFunction;
import cn.kkserver.lua.LuaState;

/**
 * Created by zhanghailong on 2017/3/6.
 */

public abstract class WeakLuaFunction<T> implements LuaFunction {

    private final WeakReference<T> _ref;

    public WeakLuaFunction(T weakObject) {
        _ref = new WeakReference<T>(weakObject);
    }

    public T weakObject() {
        return _ref.get();
    }

}

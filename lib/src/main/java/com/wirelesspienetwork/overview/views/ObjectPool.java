package com.wirelesspienetwork.overview.views;

import android.content.Context;

import java.util.Iterator;
import java.util.LinkedList;

public class ObjectPool<V, T> {

    public interface ObjectPoolConsumer<V, T> {
        public V createObject(Context context);
        public void prepareObjectToEnterPool(V v);
        public void prepareObjectToLeavePool(V v, T prepareData, boolean isNewObject);
        public boolean hasPreferredData(V v, T preferredData);
    }

    Context mContext;
    ObjectPoolConsumer<V, T> mObjectCreator;
    LinkedList<V> mPool = new LinkedList<V>();

    /** Initializes the pool with a fixed predetermined pool size */
    public ObjectPool(Context context, ObjectPoolConsumer<V, T> objectCreator) {
        mContext = context;
        mObjectCreator = objectCreator;
    }

    /** Returns a view into the pool */
    void returnObjectToPool(V v) {
        mObjectCreator.prepareObjectToEnterPool(v);
        mPool.push(v);
    }

    /** Gets a view from the pool and prepares it */
    V pickUpObjectFromPool(T preferredData, T prepareData) {
        V v = null;
        boolean isNewObject = false;
        if (mPool.isEmpty()) {
            v = mObjectCreator.createObject(mContext);
            isNewObject = true;
        } else {
            // Try and find a preferred view
            Iterator<V> iter = mPool.iterator();
            while (iter.hasNext()) {
                V vpv = iter.next();
                if (mObjectCreator.hasPreferredData(vpv, preferredData)) {
                    v = vpv;
                    iter.remove();
                    break;
                }
            }
            // Otherwise, just grab the first view
            if (v == null) {
                v = mPool.pop();
            }
        }
        mObjectCreator.prepareObjectToLeavePool(v, prepareData, isNewObject);
        return v;
    }
}

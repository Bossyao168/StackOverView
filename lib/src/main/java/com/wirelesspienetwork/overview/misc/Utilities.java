/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wirelesspienetwork.overview.misc;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/* Common code */
public class Utilities {

    // Reflection methods for altering shadows
    private static Method sPropertyMethod;
    static {
        try {
            Class<?> c = Class.forName("android.view.GLES20Canvas");
            sPropertyMethod = c.getDeclaredMethod("setProperty", String.class, String.class);
            if (!sPropertyMethod.isAccessible()) sPropertyMethod.setAccessible(true);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /** Scales a rect about its centroid */
    public static void scaleRectAboutCenter(Rect r, float scale) {
        if (scale != 1.0f) {
            int cx = r.centerX();
            int cy = r.centerY();
            r.offset(-cx, -cy);
            r.left = (int) (r.left * scale + 0.5f);
            r.top = (int) (r.top * scale + 0.5f);
            r.right = (int) (r.right * scale + 0.5f);
            r.bottom = (int) (r.bottom * scale + 0.5f);
            r.offset(cx, cy);
        }
    }

    /** Maps a coorindate in a descendant view into the parent. */
    public static float mapCoordInDescendentToSelf(View descendant, View root,
            float[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root && v != null) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v0 != descendant || includeRootScroll) {
                pt[0] -= v0.getScrollX();
                pt[1] -= v0.getScrollY();
            }

            v0.getMatrix().mapPoints(pt);
            pt[0] += v0.getLeft();
            pt[1] += v0.getTop();
            scale *= v0.getScaleX();
        }

        coord[0] = pt[0];
        coord[1] = pt[1];
        return scale;
    }

    /** Maps a coordinate in the root to a descendent. */
    public static float mapCoordInSelfToDescendent(View descendant, View root,
            float[] coord, Matrix tmpInverseMatrix) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        int count = ancestorChain.size();
        tmpInverseMatrix.set(new Matrix());
        for (int i = count - 1; i >= 0; i--) {
            View ancestor = ancestorChain.get(i);
            View next = i > 0 ? ancestorChain.get(i-1) : null;

            pt[0] += ancestor.getScrollX();
            pt[1] += ancestor.getScrollY();

            if (next != null) {
                pt[0] -= next.getLeft();
                pt[1] -= next.getTop();
                next.getMatrix().invert(tmpInverseMatrix);
                tmpInverseMatrix.mapPoints(pt);
                scale *= next.getScaleX();
            }
        }

        coord[0] = pt[0];
        coord[1] = pt[1];
        return scale;
    }

    /** Sets some private shadow properties. */
    public static void setShadowProperty(String property, String value)
            throws IllegalAccessException, InvocationTargetException {
        if (sPropertyMethod != null) {
            sPropertyMethod.invoke(null, property, value);
        }
    }
}

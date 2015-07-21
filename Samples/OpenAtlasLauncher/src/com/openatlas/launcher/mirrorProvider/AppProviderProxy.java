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
package com.openatlas.launcher.mirrorProvider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.openatlas.boot.ProviderProxy;

public class AppProviderProxy extends ProviderProxy {

    ContentProvider mDelegate;

    public AppProviderProxy() {
        super("com.openatlas.android.appcenter.AppProvider");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
//        ContentProvider mDelegate = getContentProvider();
//        if (mDelegate != null) {
//            return mDelegate.query(uri, strArr, str, strArr2, str2);
//        }
        return super.query(uri, strArr, str, strArr2, str2);
    }

    @Override
    public String getType(Uri uri) {
//        ContentProvider mDelegate = getContentProvider();
//        if (mDelegate != null) {
//            return mDelegate.getType(uri);
//        }
        return super.getType(uri);
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
//        ContentProvider mDelegate = getContentProvider();
//        if (mDelegate != null) {
//            return mDelegate.insert(uri, contentValues);
//        }
        return super.insert(uri, contentValues);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
//        ContentProvider mDelegate = getContentProvider();
//        if (mDelegate != null) {
//            return mDelegate.delete(uri, str, strArr);
//        }
        return super.delete(uri, str, strArr);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
//        ContentProvider mDelegate = getContentProvider();
//        if (mDelegate != null) {
//            return mDelegate.update(uri, contentValues, str, strArr);
//        }
        return super.update(uri, contentValues, str, strArr);
    }
}
/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.runtime.mcumgr.sample.utils;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;

public class FsUtils {
    private static final String PREFS_PARTITION = "partition";
    private static final String PARTITION_DEFAULT = "nffs";

    private final SharedPreferences mPreferences;
    private final MutableLiveData<String> mPartitionLiveData = new MutableLiveData<>();

    public FsUtils(final SharedPreferences preferences) {
        mPreferences = preferences;
        mPartitionLiveData.setValue(getPartitionString());
    }

    public LiveData<String> getPartition() {
        return mPartitionLiveData;
    }

    public String getDefaultPartition() {
        return PARTITION_DEFAULT;
    }

    public String getPartitionString() {
        return mPreferences.getString(PREFS_PARTITION, PARTITION_DEFAULT);
    }

    public void setPartition(final String partition) {
        mPreferences.edit().putString(PREFS_PARTITION, partition).apply();
        mPartitionLiveData.postValue(partition);
    }

    public static byte[] generateLoremIpsum(int size) {
        final StringBuilder builder = new StringBuilder();
        while (size > 0) {
            final int chunkSize = Math.min(LOREM.length(), size);
            builder.append(LOREM.substring(0, chunkSize));
            size -= chunkSize;
        }
        return builder.toString().getBytes();
    }

    private static final String LOREM =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque ex ante, semper ut " +
                    "faucibus pharetra, accumsan et augue. Vestibulum vulputate elit ligula, eu " +
                    "tincidunt orci lacinia quis. Proin scelerisque dui at magna placerat, id " +
                    "blandit felis vehicula. Maecenas ac nisl a odio varius condimentum luctus " +
                    "eget lorem. In non leo quis lorem faucibus pretium a sit amet sem. Aliquam " +
                    "quis mi ultrices, scelerisque arcu id, posuere neque. Nunc quis est " +
                    "efficitur, vehicula augue eget, imperdiet diam. In vitae fringilla leo. " +
                    "Sed rhoncus porttitor nunc ac lobortis. Ut quis quam urna. Curabitur " +
                    "laoreet odio risus, non pretium orci sodales sed. Vivamus eleifend accumsan " +
                    "dolor, sed tincidunt erat. Nullam sed arcu maximus, vehicula diam gravida, " +
                    "ullamcorper ligula. Donec finibus odio a vestibulum mollis. Quisque non " +
                    "metus ut justo eleifend vulputate.\n\n" +
                    "Etiam iaculis magna non bibendum eleifend. Morbi sodales lorem eros, a " +
                    "eleifend nibh accumsan vel. Integer quis ex feugiat massa venenatis " +
                    "pulvinar sed at turpis. In et orci eget mi efficitur aliquet et non odio. " +
                    "In pellentesque imperdiet convallis. Aliquam tincidunt at augue eleifend " +
                    "elementum. Sed lacinia efficitur tincidunt. Donec ac pharetra nisi, eget " +
                    "sodales tortor. Nulla imperdiet mi id mattis pharetra.\n\n" +
                    "Integer nec pretium ligula. Mauris venenatis, neque eget luctus molestie, " +
                    "neque dolor laoreet leo, ut porta felis velit a enim. Vestibulum id " +
                    "finibus enim, sit amet ullamcorper augue. Maecenas a orci non lacus " +
                    "euismod egestas. Vestibulum volutpat urna sed neque malesuada, sit amet " +
                    "pellentesque ante congue. Phasellus suscipit pellentesque felis et " +
                    "sagittis. Proin gravida ante a imperdiet suscipit. Fusce sit amet euismod " +
                    "dolor, id rhoncus mauris. Phasellus convallis ornare accumsan. Quisque " +
                    "non diam non risus rhoncus congue vel id quam. Etiam risus lacus, egestas " +
                    "a dignissim in, rhoncus in massa.\n\n" +
                    "Sed lacinia mauris neque, sed lacinia dui vehicula et. Donec sit amet " +
                    "convallis enim, eget luctus mi. Nunc elementum consequat arcu non " +
                    "condimentum. In vehicula tempus libero, quis egestas neque scelerisque ac. " +
                    "Nulla tortor leo, volutpat facilisis sagittis nec, dignissim eu tellus. " +
                    "Orci varius natoque penatibus et magnis dis parturient montes, nascetur " +
                    "ridiculus mus. Sed molestie vel est id molestie. Ut id risus ullamcorper, " +
                    "cursus mi eu, luctus dui. Integer aliquam massa sed dui luctus pharetra. " +
                    "Vestibulum at erat condimentum, posuere tortor maximus, luctus nibh. " +
                    "Nam quis mattis metus, et pretium metus. Vivamus augue magna, convallis " +
                    "ut massa sit amet, posuere viverra magna.\n\n" +
                    "In erat metus, porta nec dolor in, porttitor lacinia velit. Donec aliquam " +
                    "dolor sit amet tellus mollis varius. Mauris eget ipsum mollis, condimentum " +
                    "velit eu, elementum odio. Nam suscipit lacinia tristique. Sed neque lacus, " +
                    "porta nec est quis, scelerisque porttitor tellus. Orci varius natoque " +
                    "penatibus et magnis dis parturient montes, nascetur ridiculus mus. " +
                    "In sed volutpat eros. Donec in consequat nulla. Curabitur gravida " +
                    "condimentum dictum. Orci varius natoque penatibus et magnis dis " +
                    "parturient montes, nascetur ridiculus mus. Praesent auctor, dui quis " +
                    "bibendum bibendum, justo quam dictum diam, ac varius tortor elit nec sem. ";
}

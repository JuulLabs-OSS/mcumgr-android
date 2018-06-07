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

package io.runtime.mcumgr.sample.di.component;

import android.bluetooth.BluetoothDevice;

import dagger.BindsInstance;
import dagger.Subcomponent;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.di.McuMgrScope;
import io.runtime.mcumgr.sample.di.module.McuMgrActivitiesModule;
import io.runtime.mcumgr.sample.di.module.McuMgrFragmentBuildersModule;
import io.runtime.mcumgr.sample.di.module.McuMgrManagerModule;
import io.runtime.mcumgr.sample.di.module.McuMgrTransportModule;
import io.runtime.mcumgr.sample.di.module.McuMgrViewModelModule;

@Subcomponent(modules = {
		McuMgrActivitiesModule.class,
		McuMgrFragmentBuildersModule.class,
		McuMgrViewModelModule.class,
		McuMgrTransportModule.class,
		McuMgrManagerModule.class
})
@McuMgrScope
public interface McuMgrSubComponent {
	@Subcomponent.Builder
	interface Builder {
		/**
		 * Sets the connection target.
		 *
		 * @param device teh target Bluetooth device.
		 * @return The builder instance.
		 */
		@BindsInstance Builder target(final BluetoothDevice device);

		McuMgrSubComponent build();
	}

	/**
	 * Adds the {@link io.runtime.mcumgr.sample.MainActivity} to the
	 * {@link Dagger2Application#dispatchingAndroidInjector}.
	 * The {@link io.runtime.mcumgr.sample.MainActivity} requires the
	 * {@link io.runtime.mcumgr.McuMgrTransport} to be instantiated before injecting.
	 *
	 * @param application the application.
	 */
	void update(final Dagger2Application application);
}

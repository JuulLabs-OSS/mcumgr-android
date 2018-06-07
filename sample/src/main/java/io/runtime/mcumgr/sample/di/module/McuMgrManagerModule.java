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

package io.runtime.mcumgr.sample.di.module;

import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.managers.ConfigManager;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.managers.LogManager;
import io.runtime.mcumgr.managers.StatsManager;

@Module
public class McuMgrManagerModule {

	@Provides
	static ConfigManager provideConfigManager(final McuMgrTransport transport) {
		return new ConfigManager(transport);
	}

	@Provides
	static DefaultManager provideDefaultManager(final McuMgrTransport transport) {
		return new DefaultManager(transport);
	}

	@Provides
	static FsManager provideFsManager(final McuMgrTransport transport) {
		return new FsManager(transport);
	}

	@Provides
	static LogManager provideLogManager(final McuMgrTransport transport) {
		return new LogManager(transport);
	}

	@Provides
	static ImageManager provideImageManager(final McuMgrTransport transport) {
		return new ImageManager(transport);
	}

	@Provides
	static StatsManager provideStatsManager(final McuMgrTransport transport) {
		return new StatsManager(transport);
	}

	@Provides
	static FirmwareUpgradeManager provideFirmwareUpgradeManager(final McuMgrTransport transport) {
		return new FirmwareUpgradeManager(transport);
	}
}

/*
 * Copyright 2017, Moshe Waisberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.reactivex

import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default disposable.
 * @author Moshe Waisberg
 */
abstract class DefaultDisposable : Disposable {

    private val isDisposed = AtomicBoolean()

    override fun isDisposed(): Boolean {
        return isDisposed.get()
    }

    override fun dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            onDispose()
        }
    }

    protected abstract fun onDispose()
}
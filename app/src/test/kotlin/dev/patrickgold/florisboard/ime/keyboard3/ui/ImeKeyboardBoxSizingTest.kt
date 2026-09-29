/*
 * Copyright (C) 2026 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.ui.unit.IntSize
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImeKeyboardBoxSizingTest : FunSpec({
    test("key size keeps normal spacing") {
        visibleTouchKeySize(1000, 400, 0.1f, 0.25f, 4f, 3f) shouldBe IntSize(92, 94)
    }

    test("key size cannot turn negative while the keyboard is closing or switching") {
        visibleTouchKeySize(0, 0, 0.1f, 0.25f, 4f, 3f) shouldBe IntSize.Zero
        visibleTouchKeySize(20, 8, 0.1f, 0.25f, 4f, 3f) shouldBe IntSize.Zero
    }
})

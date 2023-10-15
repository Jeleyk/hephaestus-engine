/*
 * This file is part of hephaestus-engine, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package team.unnamed.hephaestus.animation.timeline;

import org.jetbrains.annotations.Nullable;
import team.unnamed.hephaestus.animation.interpolation.Interpolator;

public final class KeyFrame<T> {

    private final int time;
    private final T value;
    private final Interpolator<T> interpolator;

    public KeyFrame(int time, T value, @Nullable Interpolator<T> interpolator) {
        this.time = time;
        this.value = value;
        this.interpolator = interpolator;
    }

    public int time() {
        return time;
    }

    public T value() {
        return value;
    }

    public @Nullable Interpolator<T> interpolator() {
        return interpolator;
    }

    public Interpolator<T> interpolatorOr(Interpolator<T> fallback) {
        return interpolator == null ? fallback : interpolator;
    }

    @Override
    public String toString() {
        return "KeyFrame{" +
                "time=" + time +
                ", value=" + value +
                ", interpolator=" + interpolator +
                '}';
    }
}
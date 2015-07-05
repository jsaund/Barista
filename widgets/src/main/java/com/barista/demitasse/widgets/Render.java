/*
* Copyright (C) 2015 [Jag Saund]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.barista.demitasse.widgets;

/**
 * A view which provides capabilities to render the audio visualization
 * must implement the Render interface.
 */
public interface Render {
    /**
     * Set the current amplitude of the captured frequency.
     * @param amplitude
     */
    public void setAmplitude(int amplitude);

    /**
     * Returns the current amplitude for the represented frequency.
     * @return
     */
    public int getAmplitude();
}

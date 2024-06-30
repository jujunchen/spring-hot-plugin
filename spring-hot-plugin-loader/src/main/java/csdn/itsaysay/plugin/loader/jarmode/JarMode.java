/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csdn.itsaysay.plugin.loader.jarmode;

/**
 * Interface registered in {@code spring.factories} to provides extended 'jarmode'
 * support.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface JarMode {

	/**
	 * Returns if this accepts and can run the given mode.
	 * @param mode the mode to check
	 * @return if this instance accepts the mode
	 */
	boolean accepts(String mode);

	/**
	 * Run the jar in the given mode.
	 * @param mode the mode to use
	 * @param args any program arguments
	 */
	void run(String mode, String[] args);

}

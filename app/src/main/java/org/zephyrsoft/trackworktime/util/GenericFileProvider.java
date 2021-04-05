/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.util;

import androidx.core.content.FileProvider;

/**
 * Extend FileProvider to make sure our FileProvider doesn't conflict with any FileProviders declared
 * in imported dependencies as described at https://commonsware.com/blog/2017/06/27/fileprovider-libraries.html
 */
public class GenericFileProvider extends FileProvider {
    // do nothing different than the extended class
}
